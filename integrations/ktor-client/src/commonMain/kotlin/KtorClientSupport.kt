package com.rnett.krosstalk.client

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

internal class KtorClientRequestMaker(
    private val client: HttpClient,
    val requestModifier: HttpRequestBuilder.() -> Unit,
    val responseListener: (HttpResponse) -> Unit
) : RequestMaker {
    override suspend fun makeRequest(url: String, body: ByteArray): ByteArray {
        return client.post(url) {
            accept(ContentType.Application.OctetStream)
            contentType(ContentType.Application.OctetStream)
            requestModifier()
            setBody(body)
        }.also(responseListener).body()
    }
}

public fun RequestMaker(
    client: HttpClient,
    responseListener: (HttpResponse) -> Unit = {},
    requestModifier: HttpRequestBuilder.() -> Unit = {}
): RequestMaker =
    KtorClientRequestMaker(client, requestModifier, responseListener)