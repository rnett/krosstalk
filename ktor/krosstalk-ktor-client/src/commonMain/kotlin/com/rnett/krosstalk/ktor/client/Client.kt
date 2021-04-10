package com.rnett.krosstalk.ktor.client

import com.rnett.krosstalk.KrosstalkPluginApi
import com.rnett.krosstalk.client.AppliedClientScope
import com.rnett.krosstalk.client.ClientHandler
import com.rnett.krosstalk.client.ClientScope
import com.rnett.krosstalk.client.InternalKrosstalkResponse
import com.rnett.krosstalk.client.KrosstalkClient
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.receive
import io.ktor.client.features.auth.Auth
import io.ktor.client.features.auth.providers.basic
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.charset
import io.ktor.util.toMap
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.String
import io.ktor.utils.io.core.use

/**
 * Helper to apply a scope's client configuration
 */
@KrosstalkPluginApi
fun <D> AppliedClientScope<KtorClientScope<D>, *>.configureClient(client: HttpClientConfig<*>) {
    client.apply {
        scope.apply { configureClient(data as D) }
    }
}

/**
 * Helper to apply a scope's request configuration
 */
@KrosstalkPluginApi
fun <D> AppliedClientScope<KtorClientScope<D>, *>.configureRequest(request: HttpRequestBuilder) {
    request.apply {
        scope.apply { configureRequest(data as D) }
    }
}

interface KtorKrosstalkClient : KrosstalkClient<KtorClientScope<*>> {
    override val client: KtorClient
}

/**
 * A Krosstalk client using a Ktor HttpClient to make requests.
 *
 * A new client is used for each request, created by calling [HttpClient.config] on [baseClient].
 *
 * [baseRequest] is applied to each request before scopes.
 *
 */
@OptIn(KrosstalkPluginApi::class)
class KtorClient(
    val baseClient: HttpClient = HttpClient(),
    val baseRequest: HttpRequestBuilder.() -> Unit,
) : ClientHandler<KtorClientScope<*>> {

    private val realBaseClient by lazy {
        baseClient.config {
            expectSuccess = false
        }
    }

    override suspend fun sendKrosstalkRequest(
        url: String,
        httpMethod: String,
        contentType: String,
        additionalHeaders: com.rnett.krosstalk.Headers,
        body: ByteArray?,
        scopes: List<AppliedClientScope<KtorClientScope<*>, *>>,
    ): InternalKrosstalkResponse {
        // configure the client and make the request
        val response = realBaseClient.config {
            scopes.forEach {
                it.configureClient(this)
            }
        }.use { client ->
            client.request<HttpResponse>(urlString = url) {
                if (body != null)
                    this.body = body
                this.method = HttpMethod(httpMethod.toUpperCase())

                // base request configuration
                baseRequest()

                // configure scopes
                scopes.forEach {
                    it.configureRequest(this)
                }

                // add any set headers
                additionalHeaders.forEach { (key, list) ->
                    this.headers.appendAll(key, list)
                }
            }
        }

        val bytes = response.receive<ByteArray>()
        val charset = response.charset() ?: Charsets.UTF_8

        return InternalKrosstalkResponse(response.status.value, response.headers.toMap(), bytes) { String(bytes, charset = charset) }
    }

    override fun getStatusCodeName(httpStatusCode: Int): String? = HttpStatusCode.fromValue(httpStatusCode).description
}

/**
 * A Ktor client scope.
 * Allows configuration of the client and of the request.
 * Note that a new client is used for each request.
 */
@OptIn(KrosstalkPluginApi::class)
interface KtorClientScope<in D> : ClientScope<D> {
    fun HttpClientConfig<*>.configureClient(data: D) {}
    fun HttpRequestBuilder.configureRequest(data: D) {}
}

/**
 * A Ktor client scope that only alters the request.
 */
fun interface KtorClientRequestScope<D> : KtorClientScope<D> {
    override fun HttpRequestBuilder.configureRequest(data: D)
}

/**
 * A Ktor client scope that only adds headers.
 */
fun interface KtorClientHeaderScope<D> : KtorClientRequestScope<D> {
    override fun HttpRequestBuilder.configureRequest(data: D) {
        this.headers.headers(data)
    }

    fun HeadersBuilder.headers(data: D): Unit
}

/**
 * A Ktor client scope that only alters the client.
 * Note that a new client is used for each request.
 */
fun interface KtorClientClientScope<D> : KtorClientScope<D> {
    override fun HttpClientConfig<*>.configureClient(data: D)
}

/**
 * Credentials for basic auth.
 */
data class BasicCredentials(val username: String, val password: String)

abstract class KtorClientAuth<D> : KtorClientScope<D> {
    abstract fun Auth.configureClientAuth(data: D)

    override fun HttpClientConfig<*>.configureClient(data: D) {
        Auth {
            configureClientAuth(data)
        }
    }
}

open class KtorClientBasicAuth(val sendWithoutRequest: Boolean = true, val realm: String? = null) :
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