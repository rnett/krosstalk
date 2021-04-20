package com.rnett.krosstalk.ktor.server

import com.rnett.krosstalk.KrosstalkPluginApi
import com.rnett.krosstalk.server.ServerScope
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.BasicAuthenticationProvider
import io.ktor.auth.DigestAuthenticationProvider
import io.ktor.auth.DigestCredential
import io.ktor.auth.FormAuthenticationProvider
import io.ktor.auth.OAuthAccessTokenResponse
import io.ktor.auth.OAuthAuthenticationProvider
import io.ktor.auth.Principal
import io.ktor.auth.authenticate
import io.ktor.auth.authentication
import io.ktor.auth.basic
import io.ktor.auth.digest
import io.ktor.auth.digestAuthenticationCredentials
import io.ktor.auth.form
import io.ktor.auth.oauth
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

public abstract class KtorServerPrincipalAuth<T : Principal>(authName: String?) : KtorServerAuth<T>(authName) {
    override fun getData(call: ApplicationCall): T? = call.authentication.principal as T?
}

//TODO artifact for others, esp JWT?

public open class KtorServerBasicAuth<T : Principal>(
    authName: String? = randomAuthName(),
    public val configure: BasicAuthenticationProvider.Configuration.() -> Unit,
) : KtorServerPrincipalAuth<T>(authName) {

    override fun Authentication.Configuration.configureAuth() {
        basic(authName) {
            configure()
        }
    }
}

public open class KtorServerSessionAuth<T : Principal>(
    authName: String? = randomAuthName(),
    public val configure: FormAuthenticationProvider.Configuration.() -> Unit,
) : KtorServerPrincipalAuth<T>(authName) {
    override fun Authentication.Configuration.configureAuth() {
        form(authName) {
            configure()
        }
    }
}

public open class KtorServerDigestAuth<T : Principal>(
    authName: String? = randomAuthName(),
    public val configure: DigestAuthenticationProvider.Configuration.() -> Unit,
) : KtorServerAuth<DigestCredential>(authName) {
    override fun Authentication.Configuration.configureAuth() {
        digest(authName) {
            configure()
        }
    }

    override fun getData(call: ApplicationCall): DigestCredential? = call.digestAuthenticationCredentials()
}

public open class KtorServerOAuthAuth(
    authName: String? = randomAuthName(),
    public val configure: OAuthAuthenticationProvider.Configuration.() -> Unit,
) : KtorServerPrincipalAuth<OAuthAccessTokenResponse>(authName) {
    override fun Authentication.Configuration.configureAuth() {
        oauth(authName) {
            configure
        }
    }
}
