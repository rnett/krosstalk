package com.rnett.krosstalk.ktor.client

import com.rnett.krosstalk.ActiveScope
import com.rnett.krosstalk.ClientHandler
import com.rnett.krosstalk.ClientScope
import io.ktor.client.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.auth.providers.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.core.*

fun <D> ActiveScope<*, KtorClientScope<D>>.configureClient(client: HttpClientConfig<*>) {
    client.apply {
        scope.apply { configureClient(data as D) }
    }
}

fun <D> ActiveScope<*, KtorClientScope<D>>.configureRequest(request: HttpRequestBuilder) {
    request.apply {
        scope.apply { configureRequest(data as D) }
    }
}


class KtorClient(override val serverUrl: String) : ClientHandler<KtorClientScope<*>> {
    override suspend fun sendKrosstalkRequest(
        endpoint: String,
        method: String,
        body: ByteArray,
        scopes: List<ActiveScope<*, KtorClientScope<*>>>
    ): ByteArray {
        return HttpClient {
            scopes.forEach {
                it.configureClient(this)
            }
        }.use {
            it.request(urlString = "${serverUrl.trimEnd('/')}/${endpoint.trimStart('/')}") {
                this.body = body
                this.method = HttpMethod(method.toUpperCase())
                scopes.forEach {
                    it.configureRequest(this)
                }
            }
        }
    }
}


interface KtorClientScope<in D> : ClientScope<D> {
    fun HttpClientConfig<*>.configureClient(data: D) {}
    fun HttpRequestBuilder.configureRequest(data: D) {}
}

fun interface KtorClientRequestScope<D> : KtorClientScope<D> {
    override fun HttpRequestBuilder.configureRequest(data: D)
}

fun interface KtorClientClientScope<D> : KtorClientScope<D> {
    override fun HttpClientConfig<*>.configureClient(data: D)
}

data class Credentials(val username: String, val password: String)

class KtorClientBasicAuth(val sendWithoutRequest: Boolean = true, val realm: String? = null) :
    KtorClientScope<Credentials> {
    override fun HttpClientConfig<*>.configureClient(data: Credentials) {
        install(Auth) {
            basic {
                sendWithoutRequest = this@KtorClientBasicAuth.sendWithoutRequest
                username = data.username
                password = data.password
                realm = this@KtorClientBasicAuth.realm
            }
        }
    }
}