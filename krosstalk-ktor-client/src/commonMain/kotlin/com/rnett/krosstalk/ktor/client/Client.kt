package com.rnett.krosstalk.ktor.client

import com.rnett.krosstalk.ActiveScope
import com.rnett.krosstalk.ClientHandler
import com.rnett.krosstalk.ClientScope
import com.rnett.krosstalk.KrosstalkResponse
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.receive
import io.ktor.client.features.auth.Auth
import io.ktor.client.features.auth.providers.basic
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpMethod
import io.ktor.http.isSuccess
import io.ktor.utils.io.core.use

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
            httpMethod: String,
            body: ByteArray?,
            scopes: List<ActiveScope<*, KtorClientScope<*>>>
    ): KrosstalkResponse {
        HttpClient {
            scopes.forEach {
                it.configureClient(this)
            }
            expectSuccess = false
        }.use {
            val response = it.request<HttpResponse>(urlString = "${serverUrl.trimEnd('/')}/${endpoint.trimStart('/')}") {
                if (body != null)
                    this.body = body
                this.method = HttpMethod(httpMethod.toUpperCase())
                scopes.forEach {
                    it.configureRequest(this)
                }
            }

            val status = response.status

            return if (status.isSuccess())
                KrosstalkResponse.Success(status.value, response.receive())
            else
                KrosstalkResponse.Failure(status.value) {
                    //TODO try to receive string?  Seems like a possibly bad idea
                    error("Krosstalk method $it failed with: $status")
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