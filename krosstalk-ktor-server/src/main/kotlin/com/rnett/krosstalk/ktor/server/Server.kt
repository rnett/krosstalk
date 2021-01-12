package com.rnett.krosstalk.ktor.server

import com.rnett.krosstalk.*
import com.rnett.krosstalk.ktor.server.KtorServer.define
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.*
import io.ktor.http.HttpMethod
import io.ktor.request.receiveChannel
import io.ktor.request.uri
import io.ktor.response.respondBytes
import io.ktor.routing.Route
import io.ktor.routing.method
import io.ktor.routing.routing
import io.ktor.util.toByteArray
import kotlin.random.Random

/**
 * Applies [remaining] scopes in reverse order, recursively, with [final] inside all of them.
 */
fun wrapScopesHelper(route: Route, remaining: MutableList<NeededScope<KtorServerScope>>, final: Route.() -> Unit) {
    if (remaining.isEmpty())
        route.final()
    else {
        val scope = remaining.removeLast()

        scope.scope.apply {
            route.wrapEndpoint(scope.optional) {
                wrapScopesHelper(this, remaining, final)
            }
        }
    }
}

/**
 * Applies [remaining] scopes, recursively, with [final] inside all of them.
 */
fun wrapScopes(route: Route, remaining: List<NeededScope<KtorServerScope>>, final: Route.() -> Unit) =
    wrapScopesHelper(route, remaining.toMutableList().asReversed(), final)

interface KtorKrosstalkServer : KrosstalkServer<KtorServerScope> {
    override val server: KtorServer
}

/**
 * A Krosstalk server handler that adds the krosstalk method endpoints to a Ktor server.
 */
object KtorServer : ServerHandler<KtorServerScope> {

    /**
     * Meant to be called from an Application, like:
     * ```kotlin
     * fun Application.server(){
     *     KtorServer.define(this, MyKrosstalk)
     * }
     * ```
     *
     * @see [defineKtor]
     */
    fun <K> define(app: Application, krosstalk: K) where K : Krosstalk, K : KrosstalkServer<KtorServerScope> {
        // apply Application configuration for each defined scopes
        app.apply {
            krosstalk.scopes.values
                    .forEach {
                        it.scope.apply {
                            configureApplication()
                        }
                    }
        }

        app.routing {
            // add each method
            krosstalk.methods.forEach { (name, method) ->
                // wrap the endpoint in the needed scopes
                wrapScopes(this, krosstalk.neededServerScopes(method)) {
                    method(HttpMethod(method.httpMethod)) {
                        handle {
                            val data = method.endpoint.resolve(call.request.uri) ?: return@handle
                            val body = call.receiveChannel().toByteArray()
                            val response = krosstalk.handle(name, data, body)
                            call.respondBytes(response)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Meant to be called from an Application, like:
 * ```kotlin
 * fun Application.server(){
 *     MyKrosstalk.defineKtor(this)
 * }
 * ```
 *
 * @see [define]
 */
fun <K> K.defineKtor(app: Application) where K : Krosstalk, K : KrosstalkServer<KtorServerScope> {
    KtorServer.define(app, this)
}

/**
 * A Ktor server scope.  Supports configuring the application and wrapping endpoints.
 */
interface KtorServerScope : ServerScope {
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

    // can just use handle in buildEndpoint, I think
//    fun PipelineContext<Unit, ApplicationCall>.handleRequest() {}
}


abstract class KtorServerAuth(val authName: String? = randomAuthName()) : KtorServerScope {
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
        authenticate(authName) {
            endpoint()
        }
    }
}

class KtorServerBasicAuth(authName: String? = randomAuthName(), val configure: BasicAuthenticationProvider.Configuration.() -> Unit) : KtorServerAuth(authName) {
    override fun Authentication.Configuration.configureAuth() {
        basic(authName) {
            configure()
        }
    }
}

class KtorServerSessionAuth(authName: String? = randomAuthName(), val configure: FormAuthenticationProvider.Configuration.() -> Unit) : KtorServerAuth(authName) {
    override fun Authentication.Configuration.configureAuth() {
        form(authName) {
            configure()
        }
    }
}

class KtorServerDigestAuth(authName: String? = randomAuthName(), val configure: DigestAuthenticationProvider.Configuration.() -> Unit) : KtorServerAuth(authName) {
    override fun Authentication.Configuration.configureAuth() {
        digest(authName) {
            configure()
        }
    }
}

class KtorServerOAuthAuth(authName: String? = randomAuthName(), val configure: OAuthAuthenticationProvider.Configuration.() -> Unit) : KtorServerAuth(authName) {
    override fun Authentication.Configuration.configureAuth() {
        oauth(authName) {
            configure
        }
    }
}