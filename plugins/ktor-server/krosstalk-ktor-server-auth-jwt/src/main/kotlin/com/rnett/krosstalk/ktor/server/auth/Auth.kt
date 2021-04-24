package com.rnett.krosstalk.ktor.server.auth

import io.ktor.auth.Authentication
import io.ktor.auth.Principal
import io.ktor.auth.jwt.JWTAuthenticationProvider
import io.ktor.auth.jwt.jwt

/**
 * A Ktor server JWT auth scope.
 */
public abstract class KtorServerJWTAuth<T : Principal>(
    authName: String? = randomAuthName()
) : KtorServerPrincipalAuth<T>(authName) {
    public abstract fun JWTAuthenticationProvider.Configuration.configure()

    override fun Authentication.Configuration.configureAuth() {
        jwt(authName) {
            configure()
        }
    }
}
