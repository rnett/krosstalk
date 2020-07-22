package com.rnett.krosstalk.ktor

import com.rnett.krosstalk.ClientHandler
import com.rnett.krosstalk.ClientScope
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.js.Js
import io.ktor.client.features.auth.Auth
import io.ktor.client.features.auth.providers.basic
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.post


object KtorClient : ClientHandler<KtorClientScope> {
    override suspend fun sendKrosstalkRequest(methodName: String, body: ByteArray, scopes: List<KtorClientScope>): ByteArray {
        return HttpClient(Js) {
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
    fun HttpClientConfig<HttpClientEngineConfig>.configureClient() {}
    fun HttpRequestBuilder.configureRequest() {}
}

data class KtorClientAuth(val username: String, val password: String) : KtorClientScope {
    override fun HttpClientConfig<HttpClientEngineConfig>.configureClient() {
        install(Auth) {
            basic {
                sendWithoutRequest = true
                username = this@KtorClientAuth.username
                password = this@KtorClientAuth.password
            }
        }
    }
}