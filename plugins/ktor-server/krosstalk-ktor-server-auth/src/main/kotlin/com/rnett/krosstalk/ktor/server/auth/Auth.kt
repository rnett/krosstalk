package com.rnett.krosstalk.ktor.server.auth

import com.rnett.krosstalk.ktor.server.KtorServerScope
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
    @Suppress("UNCHECKED_CAST")
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
