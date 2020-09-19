package com.rnett.krosstalk.ktor.client

import com.rnett.krosstalk.ClientHandler
import com.rnett.krosstalk.ClientScope
import io.ktor.client.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.auth.providers.*
import io.ktor.client.request.*


object KtorClient : ClientHandler<KtorClientScope> {
    override suspend fun sendKrosstalkRequest(methodName: String, body: ByteArray, scopes: List<KtorClientScope>): ByteArray {
        return HttpClient {
            scopes.forEach {
                it.apply { configureClient() }
            }
        }.post(path = "/krosstalk/$methodName", port = 8080) {
            this.body = body
            scopes.forEach {
                it.apply { configureRequest() }
            }
        }
    }
}


interface KtorClientScope : ClientScope {
    fun HttpClientConfig<*>.configureClient() {}
    fun HttpRequestBuilder.configureRequest() {}
}

data class KtorClientAuth(val username: String, val password: String) : KtorClientScope {
    override fun HttpClientConfig<*>.configureClient() {
        install(Auth) {
            basic {
                sendWithoutRequest = true
                username = this@KtorClientAuth.username
                password = this@KtorClientAuth.password
            }
        }
    }
}