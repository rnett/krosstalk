package com.rnett.krosstalk.ktor.server

import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.KrosstalkPluginApi
import com.rnett.krosstalk.ktor.server.KtorServer.define
import com.rnett.krosstalk.server.KrosstalkServer
import com.rnett.krosstalk.server.MutableWantedScopes
import com.rnett.krosstalk.server.ServerHandler
import com.rnett.krosstalk.server.ServerScope
import com.rnett.krosstalk.server.handle
import com.rnett.krosstalk.server.scopesAsType
import com.rnett.krosstalk.server.serverScopes
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.auth.Authentication
import io.ktor.auth.BasicAuthenticationProvider
import io.ktor.auth.DigestAuthenticationProvider
import io.ktor.auth.FormAuthenticationProvider
import io.ktor.auth.OAuthAuthenticationProvider
import io.ktor.auth.authenticate
import io.ktor.auth.basic
import io.ktor.auth.digest
import io.ktor.auth.form
import io.ktor.auth.oauth
import io.ktor.http.HttpMethod
import io.ktor.request.receiveChannel
import io.ktor.request.uri
import io.ktor.response.respondBytes
import io.ktor.routing.Route
import io.ktor.routing.application
import io.ktor.routing.method
import io.ktor.routing.route
import io.ktor.util.toByteArray
import kotlin.random.Random

/**
 * Applies [remaining] scopes in reverse order, recursively, with [final] inside all of them.
 */
fun wrapScopesHelper(
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
fun wrapScopes(route: Route, optional: Boolean, remaining: List<KtorServerScope<*>>, final: Route.() -> Unit) =
    wrapScopesHelper(route, optional, remaining.toMutableList().asReversed(), final)

interface KtorKrosstalkServer : KrosstalkServer<KtorServerScope<*>> {
    override val server: KtorServer
}

/**
 * A Krosstalk server handler that adds the krosstalk method endpoints to a Ktor server.
 */
@OptIn(KrosstalkPluginApi::class)
object KtorServer : ServerHandler<KtorServerScope<*>> {

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
    fun <K> define(base: Route, krosstalk: K) where K : Krosstalk, K : KrosstalkServer<KtorServerScope<*>> {
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
                // wrap the endpoint in the needed scopes
                wrapScopes(
                    this,
                    true,
                    method.requiredScopes.let(krosstalk::scopesAsType)
                ) {

                    wrapScopes(
                        this,
                        false,
                        method.optionalScopes.let(krosstalk::scopesAsType)
                    ) {

                        method(HttpMethod(method.httpMethod)) {
                            route("{...}") {
                                handle {
                                    val data = method.endpoint.resolve(call.request.uri) ?: return@handle
                                    val body = call.receiveChannel().toByteArray()

                                    val scopes = MutableWantedScopes()
                                    method.requiredScopes.let(krosstalk::scopesAsType).forEach {
                                        scopes[it as KtorServerScope<Any?>] =
                                            it.getData(call) ?: error("Required scope $it didn't get any data")
                                    }
                                    method.optionalScopes.let(krosstalk::scopesAsType).forEach { scope ->
                                        scope.getData(call)?.let { scopes[scope as KtorServerScope<Any?>] = it }
                                    }

                                    krosstalk.handle(method.name, data, body, scopes.toImmutable(), {
                                        application.log.error("Server exception during ${method.name}, passed on to client", it)
                                    }) { call.respondBytes(it) }
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

//TODO use multiple receivers, add one for Application
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
fun <K> K.defineKtor(route: Route) where K : Krosstalk, K : KrosstalkServer<KtorServerScope<*>> {
    KtorServer.define(route, this)
}

/**
 * A Ktor server scope.  Supports configuring the application and wrapping endpoints.
 */
interface KtorServerScope<S> : ServerScope<S> {
    /**
     * Configure the Ktor application.
     */
    fun Application.configureApplication() {}

    /**
     * Wrap the endpoint.  **[endpoint] must be called at some point, it will build the rest of the endpoint.**
     *
     * @param optional Whether the scope is a optional scope for the method it is being applied to.
     * @param endpoint Builder for the rest of the endpoint.
     */
    fun Route.wrapEndpoint(optional: Boolean, endpoint: Route.() -> Unit) {}

    /**
     * Will throw an error if it returns null but the scope is non-optional
     */
    fun getData(call: ApplicationCall): S?

    // can just use handle in buildEndpoint, I think
//    fun PipelineContext<Unit, ApplicationCall>.handleRequest() {}
}


abstract class KtorServerAuth(val authName: String? = randomAuthName()) : KtorServerScope<Unit> {
    companion object {
        private val usedNames = mutableSetOf<String>()
        fun randomAuthName(): String {
            while (true) {
                val name = "auth-${Random.nextInt(0, Int.MAX_VALUE)}"
                if (name !in usedNames) {
                    usedNames += name
                    return name
                }
            }
        }
    }

    abstract fun Authentication.Configuration.configureAuth()

    override fun Application.configureApplication() {
        install(Authentication) {
            configureAuth()
        }
    }

    override fun Route.wrapEndpoint(optional: Boolean, endpoint: Route.() -> Unit) {
        authenticate(authName, optional = optional) {
            endpoint()
        }
    }

    override fun getData(call: ApplicationCall) = Unit
}

class KtorServerBasicAuth(
    authName: String? = randomAuthName(),
    val configure: BasicAuthenticationProvider.Configuration.() -> Unit,
) : KtorServerAuth(authName) {
    override fun Authentication.Configuration.configureAuth() {
        basic(authName) {
            configure()
        }
    }
}

class KtorServerSessionAuth(
    authName: String? = randomAuthName(),
    val configure: FormAuthenticationProvider.Configuration.() -> Unit,
) : KtorServerAuth(authName) {
    override fun Authentication.Configuration.configureAuth() {
        form(authName) {
            configure()
        }
    }
}

class KtorServerDigestAuth(
    authName: String? = randomAuthName(),
    val configure: DigestAuthenticationProvider.Configuration.() -> Unit,
) : KtorServerAuth(authName) {
    override fun Authentication.Configuration.configureAuth() {
        digest(authName) {
            configure()
        }
    }
}

class KtorServerOAuthAuth(
    authName: String? = randomAuthName(),
    val configure: OAuthAuthenticationProvider.Configuration.() -> Unit,
) : KtorServerAuth(authName) {
    override fun Authentication.Configuration.configureAuth() {
        oauth(authName) {
            configure
        }
    }
}