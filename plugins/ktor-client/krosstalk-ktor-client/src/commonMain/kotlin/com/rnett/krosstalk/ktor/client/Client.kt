package com.rnett.krosstalk.ktor.client

import com.rnett.krosstalk.KrosstalkPluginApi
import com.rnett.krosstalk.client.KrosstalkClient
import com.rnett.krosstalk.client.plugin.AppliedClientScope
import com.rnett.krosstalk.client.plugin.ClientHandler
import com.rnett.krosstalk.client.plugin.InternalKrosstalkResponse
import com.rnett.krosstalk.toHeaders
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.receive
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpMethod
import io.ktor.http.charset
import io.ktor.util.toMap
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.String
import io.ktor.utils.io.core.use

/**
 * Helper to apply a scope's client configuration
 */
@KrosstalkPluginApi
internal fun <D> AppliedClientScope<KtorClientScope<D>, *>.configureClient(client: HttpClientConfig<*>) {
    client.apply {
        scope.apply { configureClient(data as D) }
    }
}

/**
 * Helper to apply a scope's request configuration
 */
@KrosstalkPluginApi
internal fun <D> AppliedClientScope<KtorClientScope<D>, *>.configureRequest(request: HttpRequestBuilder) {
    request.apply {
        scope.apply { configureRequest(data as D) }
    }
}

public interface KtorKrosstalkClient : KrosstalkClient<KtorClientScope<*>> {
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
public class KtorClient(
    public val baseClient: HttpClient = HttpClient(),
    public val baseRequest: HttpRequestBuilder.() -> Unit = {},
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
                this.method = HttpMethod(httpMethod.uppercase())

                // base request configuration
                baseRequest()

                // configure scopes
                scopes.forEach {
                    it.configureRequest(this)
                }

                // add any set headers
                additionalHeaders.forEach { key, list ->
                    this.headers.appendAll(key, list)
                }
            }
        }

        val bytes = response.receive<ByteArray>()
        val charset = response.charset() ?: Charsets.UTF_8

        return InternalKrosstalkResponse(response.status.value, response.headers.toMap().toHeaders(), bytes) {
            String(
                bytes,
                charset = charset
            )
        }
    }
}