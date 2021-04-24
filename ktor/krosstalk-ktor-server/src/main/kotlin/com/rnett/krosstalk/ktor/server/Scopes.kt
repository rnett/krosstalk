package com.rnett.krosstalk.ktor.server

import com.rnett.krosstalk.KrosstalkPluginApi
import com.rnett.krosstalk.server.plugin.ServerScope
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.install
import io.ktor.auth.*
import io.ktor.http.Headers
import io.ktor.routing.Route
import kotlin.random.Random


/**
 * A Ktor server scope.  Supports configuring the application and wrapping endpoints.
 */
@OptIn(KrosstalkPluginApi::class)
public interface KtorServerScope<S : Any> : ServerScope<S> {
    /**
     * Configure the Ktor application.
     */
    public fun Application.configureApplication() {}

    /**
     * Wrap the endpoint.  **[endpoint] must be called at some point, it will build the rest of the endpoint.**
     *
     * @param optional Whether the scope is a optional scope for the method it is being applied to.
     * @param endpoint Builder for the rest of the endpoint.
     */
    public fun Route.wrapEndpoint(optional: Boolean, endpoint: Route.() -> Unit) {}

    /**
     * Get the scope's data.
     * Will throw an error if it returns null but the scope is non-optional
     *
     * TODO use a better way than nullability, I want to support nullable data
     */
    public fun getData(call: ApplicationCall): S?

    // can just use handle in buildEndpoint, I think
//    fun PipelineContext<Unit, ApplicationCall>.handleRequest() {}
}

/**
 * A Ktor server scope that only reads headers.
 */
public interface KtorServerHeaderScope<T : Any> : KtorServerScope<T> {
    public abstract fun getValue(headers: Headers): T?

    override fun getData(call: ApplicationCall): T? {
        return getValue(call.request.headers)
    }
}

/**
 * A base class for defining an authentication scope.
 */
public abstract class KtorServerAuth<T : Any>(public val authName: String? = randomAuthName()) : KtorServerScope<T> {
    public companion object {
        private val usedNames = mutableSetOf<String>()
        public fun randomAuthName(): String {
            while (true) {
                val name = "auth-${Random.nextInt(0, Int.MAX_VALUE)}"
                if (name !in usedNames) {
                    usedNames += name
                    return name
                }
            }
        }
    }

    public abstract fun Authentication.Configuration.configureAuth()

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
}

/**
 * A base class for defining an authentication scope that sets a principal to `call.authentication.principal`.
 */
public abstract class KtorServerPrincipalAuth<T : Principal>(authName: String?) : KtorServerAuth<T>(authName) {
    override fun getData(call: ApplicationCall): T? = call.authentication.principal as T?
}

/**
 * A Ktor server Basic auth scope.
 */
public abstract class KtorServerBasicAuth<T : Principal>(
    authName: String? = randomAuthName()
) : KtorServerPrincipalAuth<T>(authName) {
    public abstract fun BasicAuthenticationProvider.Configuration.configure()

    override fun Authentication.Configuration.configureAuth() {
        basic(authName) {
            configure()
        }
    }
}

/**
 * A Ktor server form auth scope.
 */
public abstract class KtorServerFormAuth<T : Principal>(
    authName: String? = randomAuthName(),
) : KtorServerPrincipalAuth<T>(authName) {
    public abstract fun FormAuthenticationProvider.Configuration.configure()

    override fun Authentication.Configuration.configureAuth() {
        form(authName) {
            configure()
        }
    }
}

/**
 * A Ktor server digest auth scope.
 */
public abstract class KtorServerDigestAuth<T : Principal>(
    authName: String? = randomAuthName(),
) : KtorServerAuth<DigestCredential>(authName) {
    public abstract fun DigestAuthenticationProvider.Configuration.configure()
    override fun Authentication.Configuration.configureAuth() {
        digest(authName) {
            configure()
        }
    }

    override fun getData(call: ApplicationCall): DigestCredential? = call.digestAuthenticationCredentials()
}

/**
 * A Ktor server OAuth auth scope.
 */
public abstract class KtorServerOAuthAuth(
    authName: String? = randomAuthName()
) : KtorServerPrincipalAuth<OAuthAccessTokenResponse>(authName) {
    public abstract fun OAuthAuthenticationProvider.Configuration.configure()
    override fun Authentication.Configuration.configureAuth() {
        oauth(authName) {
            configure()
        }
    }
}
