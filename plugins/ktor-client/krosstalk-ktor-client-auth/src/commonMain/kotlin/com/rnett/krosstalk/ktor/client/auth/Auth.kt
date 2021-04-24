package com.rnett.krosstalk.ktor.client.auth

import com.rnett.krosstalk.ScopeInstance
import com.rnett.krosstalk.client.invoke
import com.rnett.krosstalk.ktor.client.KtorClientScope
import io.ktor.client.HttpClientConfig
import io.ktor.client.features.auth.Auth
import io.ktor.client.features.auth.providers.basic

/**
 * Credentials for basic auth.
 */
public data class BasicCredentials(val username: String, val password: String)

/**
 * A base Ktor client authentication scope.
 */
public abstract class KtorClientAuth<D> : KtorClientScope<D> {
    public abstract fun Auth.configureClientAuth(data: D)

    override fun HttpClientConfig<*>.configureClient(data: D) {
        Auth {
            configureClientAuth(data)
        }
    }
}

/**
 * A Ktor client Basic authentication scope.
 */
public open class KtorClientBasicAuth(public val sendWithoutRequest: Boolean = true, public val realm: String? = null) :
    KtorClientAuth<BasicCredentials>() {
    override fun Auth.configureClientAuth(data: BasicCredentials) {
        basic {
            sendWithoutRequest = this@KtorClientBasicAuth.sendWithoutRequest
            username = data.username
            password = data.password
            realm = this@KtorClientBasicAuth.realm
        }
    }
}

/**
 * Create a basic auth scope instance by providing the username and password.
 */
public operator fun <T : KtorClientBasicAuth> T.invoke(username: String, password: String): ScopeInstance<T> =
    this.invoke(BasicCredentials(username, password))