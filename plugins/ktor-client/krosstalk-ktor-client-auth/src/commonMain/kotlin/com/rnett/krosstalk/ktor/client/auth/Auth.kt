package com.rnett.krosstalk.ktor.client.auth

import com.rnett.krosstalk.ScopeInstance
import com.rnett.krosstalk.ktor.client.KtorClientScope
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BasicAuthCredentials
import io.ktor.client.plugins.auth.providers.BearerAuthConfig
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.basic
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.statement.HttpResponse
import com.rnett.krosstalk.client.invoke as makeScope

/**
 * A base Ktor client authentication scope.
 */
public abstract class KtorClientAuth<D> : KtorClientScope<D> {
    public abstract fun Auth.configureClientAuth(data: D)

    final override fun HttpClientConfig<*>.configureClient(data: D) {
        Auth {
            configureClientAuth(data)
        }
    }
}

/**
 * A Ktor client Basic authentication scope.
 */
public open class KtorClientBasicAuth(public val sendWithoutRequest: Boolean = true, public val realm: String? = null) :
    KtorClientAuth<BasicAuthCredentials>() {
    final override fun Auth.configureClientAuth(data: BasicAuthCredentials) {
        basic {
            credentials { data }
            sendWithoutRequest { this@KtorClientBasicAuth.sendWithoutRequest }
            realm = this@KtorClientBasicAuth.realm
        }
    }
}

/**
 * Create a basic auth scope instance by providing the username and password.
 */
public operator fun <T : KtorClientBasicAuth> T.invoke(username: String, password: String): ScopeInstance<T> =
    this.makeScope(BasicAuthCredentials(username, password))

/**
 * A Ktor client Bearer authentication scope that loads tokens based on passed data.
 */
public abstract class KtorClientBearerAuth<T>(public val sendWithoutRequest: Boolean = true, public val realm: String? = null) :
    KtorClientAuth<T>() {

    /**
     * Used as Ktor's [BearerAuthConfig.loadTokens].
     */
    public abstract suspend fun loadTokens(data: T): BearerTokens?

    /**
     * Used as Ktor's [BearerAuthConfig.refreshTokens].  Calls [loadTokens] by default.
     */
    public open suspend fun refreshTokens(data: T, response: HttpResponse, client: HttpClient, oldTokens: BearerTokens?): BearerTokens? = loadTokens(data)

    final override fun Auth.configureClientAuth(data: T) {
        bearer {
            this.loadTokens{ loadTokens(data) }
            this.refreshTokens{
                refreshTokens(data, this.response, this.client, this.oldTokens)
            }
            sendWithoutRequest { this@KtorClientBearerAuth.sendWithoutRequest }
            realm = this@KtorClientBearerAuth.realm
        }
    }
}

/**
 * Bearer auth tokens, similar to [BearerTokens] except the refresh token is optional.
 */
public data class BearerAuthTokens(val accessToken: String, val refreshToken: String?){
    /**
     * Get the Ktor access tokens, returning an empty refresh token if [refreshToken] is `null`.
     */
    public fun accessTokens(): BearerTokens = BearerTokens(accessToken, refreshToken.orEmpty())

    /**
     * Get the Ktor refresh tokens, returning `null` if [refreshToken] is `null`.
     */
    public fun refreshTokens(): BearerTokens? = refreshToken?.let { accessTokens() }
}

/**
 * A Ktor client Bearer authentication scope that is passed tokens.
 */
public open class KtorClientBearerTokenAuth(sendWithoutRequest: Boolean = true, realm: String? = null):
    KtorClientBearerAuth<BearerAuthTokens>(sendWithoutRequest, realm){
    final override suspend fun loadTokens(data: BearerAuthTokens): BearerTokens = data.accessTokens()

    final override suspend fun refreshTokens(data: BearerAuthTokens, response: HttpResponse, client: HttpClient, oldTokens: BearerTokens?): BearerTokens? =
        data.refreshTokens()
}

/**
 * Create a bearer auth scope instance by providing the access token and optionally a refresh token.
 */
public operator fun <T : KtorClientBearerTokenAuth> T.invoke(accessToken: String, refreshToken: String? = null): ScopeInstance<T> =
    this.makeScope(BearerAuthTokens(accessToken, refreshToken))