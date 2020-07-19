package com.rnett.krosstalk.ktor

import com.rnett.krosstalk.ClientHandler
import com.rnett.krosstalk.ClientScope
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.js.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.auth.providers.*
import io.ktor.client.request.*


actual object KtorClient : ClientHandler<KtorClientScope> {
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


actual interface KtorClientScope : ClientScope {
    fun HttpClientConfig<HttpClientEngineConfig>.configureClient(){}
    fun HttpRequestBuilder.configureRequest(){}
}

actual data class KtorClientAuth(actual val username: String, actual val password: String) : KtorClientScope {
    override fun HttpClientConfig<HttpClientEngineConfig>.configureClient() {
        install(Auth){
            basic {
                sendWithoutRequest = true
                username = this@KtorClientAuth.username
                password = this@KtorClientAuth.password
            }
        }
    }
}