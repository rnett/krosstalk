package com.rnett.krosstalk.ktor.server

import com.rnett.krosstalk.Headers
import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.KrosstalkPluginApi
import com.rnett.krosstalk.MethodDefinition
import com.rnett.krosstalk.endpoint.UrlRequest
import com.rnett.krosstalk.ktor.server.KtorServer.define
import com.rnett.krosstalk.server.KrosstalkServer
import com.rnett.krosstalk.server.MutableWantedScopes
import com.rnett.krosstalk.server.ServerHandler
import com.rnett.krosstalk.server.handle
import com.rnett.krosstalk.server.scopesAsType
import com.rnett.krosstalk.server.serverScopes
import io.ktor.application.Application
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.application.log
import io.ktor.http.BadContentTypeFormatException
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.request.httpMethod
import io.ktor.request.receiveChannel
import io.ktor.request.uri
import io.ktor.response.respondBytes
import io.ktor.routing.Route
import io.ktor.routing.RouteSelector
import io.ktor.routing.RouteSelectorEvaluation
import io.ktor.routing.RoutingResolveContext
import io.ktor.routing.application
import io.ktor.routing.routing
import io.ktor.util.AttributeKey
import io.ktor.util.createFromCall
import io.ktor.util.toByteArray
import io.ktor.util.toMap

/**
 * Applies [remaining] scopes in reverse order, recursively, with [final] inside all of them.
 */
internal fun wrapScopesHelper(
    route: Route,
    optional: Boolean,
    remaining: MutableList<KtorServerScope<*>>,
    final: Route.() -> Unit,
) {
    if (remaining.isEmpty())
        route.final()
    else {
        val scope = remaining.removeLast()

        scope.apply {
            route.wrapEndpoint(optional) {
                wrapScopesHelper(this, optional, remaining, final)
            }
        }
    }
}

/**
 * Applies [remaining] scopes, recursively, with [final] inside all of them.
 */
internal fun wrapScopes(route: Route, optional: Boolean, remaining: List<KtorServerScope<*>>, final: Route.() -> Unit) =
    wrapScopesHelper(route, optional, remaining.toMutableList().asReversed(), final)

public interface KtorKrosstalkServer : KrosstalkServer<KtorServerScope<*>> {
    override val server: KtorServer
}

/**
 * A Krosstalk server handler that adds the krosstalk method endpoints to a Ktor server.
 */
@OptIn(KrosstalkPluginApi::class)
public object KtorServer : ServerHandler<KtorServerScope<*>> {

    /**
     * Defines the necessary routes for [krosstalk]'s methods.
     *
     * **If called in a non-root route, ensure that the client is configured to call the right URLs.**
     * You can set [Krosstalk.prefix] differently or set the URL in the client's server (how will depend on your client implementation).
     *
     * Meant to be called from an Application, like:
     * ```kotlin
     * fun Application.server(){
     *     routing{
     *         MyKrosstalk.defineKtor(this)
     *     }
     * }
     * ```
     *
     * @see [defineKtor]
     */
    public fun <K> define(base: Route, krosstalk: K) where K : Krosstalk, K : KrosstalkServer<KtorServerScope<*>> {
        // apply Application configuration for each defined scopes
        base.application.apply {
            krosstalk.serverScopes
                .forEach {
                    it.apply {
                        configureApplication()
                    }
                }
        }

        base.apply {


            // add each method
            krosstalk.methods.values.forEach { method ->
                this.createChild(KrosstalkRouteSelector(method)).apply {
                    // wrap the endpoint in the needed scopes
                    wrapScopes(
                        this,
                        false,
                        method.requiredScopes.let(krosstalk::scopesAsType)
                    ) {

                        wrapScopes(
                            this,
                            true,
                            method.optionalScopes.let(krosstalk::scopesAsType)
                        ) {

                            handle {
                                val data = call.attributes[KrosstalkMethodAttribute]
                                val body = call.receiveChannel().toByteArray()

                                val scopes = MutableWantedScopes()

                                method.allScopes.let(krosstalk::scopesAsType).forEach { scope ->
                                    scope.getData(call)?.let { scopes[scope as KtorServerScope<Any>] = it }
                                }

                                krosstalk.handle(call.attributes[KrosstalkMethodBaseUrlAttribute],
                                    method,
                                    call.request.headers.toMap(),
                                    data,
                                    body,
                                    scopes.toImmutable(),
                                    {
                                        application.log.error("Server exception during ${method.name}, passed on to client", it)
                                    }) { status: Int, contentType: String?, headers: Headers, bytes: ByteArray ->

                                    headers.forEach { (k, v) ->
                                        v.forEach {
                                            call.response.headers.append(k, it, false)
                                        }
                                    }

                                    call.respondBytes(
                                        bytes,
                                        contentType?.let {
                                            try {
                                                ContentType.parse(it)
                                            } catch (t: BadContentTypeFormatException) {
                                                null
                                            }
                                        },
                                        HttpStatusCode.fromValue(status)
                                    )
                                    this.finish()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val KrosstalkMethodAttribute = AttributeKey<Map<String, String>>("KrosstalkMethodData")
private val KrosstalkMethodBaseUrlAttribute = AttributeKey<String>("KrosstalkMethodBaseUrl")

internal class KrosstalkRouteSelector(val method: MethodDefinition<*>) : RouteSelector(2.0) {
    override fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation {
        with(context) {
            if (call.request.httpMethod.value.toLowerCase() != method.httpMethod.toLowerCase()) {
                return RouteSelectorEvaluation.Failed
            }

            val prefix = segments.take(segmentIndex)

            val localUrl = UrlRequest(call.request.uri).withoutPrefixParts(prefix)

            val baseUrl = URLBuilder.createFromCall(call).buildString().substringBefore(localUrl.urlParts.joinToString("/"))

            val data = method.endpoint.resolve(localUrl) ?: return RouteSelectorEvaluation.Failed
            call.attributes.put(KrosstalkMethodAttribute, data)
            call.attributes.put(KrosstalkMethodBaseUrlAttribute, baseUrl)
            return RouteSelectorEvaluation(true, 2.0, segmentIncrement = segments.size - segmentIndex)
        }
    }

    override fun toString(): String {
        return "(Krosstalk method: ${method.name})"
    }

}

//TODO use multiple receivers
/**
 * Defines the necessary routes for [this]'s methods.
 *
 * **If called in a non-root route, ensure that the client is configured to call the right URLs.**
 * You can set [Krosstalk.prefix] differently or set the URL in the client's server (how will depend on your client implementation).
 *
 * Meant to be called from an Application, like:
 * ```kotlin
 * fun Application.server(){
 *     routing{
 *         MyKrosstalk.defineKtor(this)
 *     }
 * }
 * ```
 *
 * @see [define]
 */
public fun <K> K.defineKtor(route: Route) where K : Krosstalk, K : KrosstalkServer<KtorServerScope<*>> {
    KtorServer.define(route, this)
}

/**
 * Defines the necessary routes for [this]'s methods.
 *
 * **If called in a non-root route, ensure that the client is configured to call the right URLs.**
 * You can set [Krosstalk.prefix] differently or set the URL in the client's server (how will depend on your client implementation).
 *
 * Meant to be called from an Application, like:
 * ```kotlin
 * fun Application.server(){
 *     routing{
 *         MyKrosstalk.defineKtor(this)
 *     }
 * }
 * ```
 *
 * @see [define]
 */
public fun <K> K.defineKtor(application: Application) where K : Krosstalk, K : KrosstalkServer<KtorServerScope<*>> {
    KtorServer.define(application.routing { }, this)
}
