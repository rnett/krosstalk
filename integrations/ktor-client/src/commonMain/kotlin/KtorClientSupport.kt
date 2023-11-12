package com.rnett.krosstalk.client

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

internal class KtorClientRequestMaker(
    private val client: HttpClient,
    val requestModifier: HttpRequestBuilder.() -> Unit,
    val responseListener: (HttpResponse) -> Unit
) : RequestMaker {
    override suspend fun makeRequest(url: String, body: ByteArray): ResultAndStatus {
        return client.post(url) {
            accept(ContentType.Application.OctetStream)
            contentType(ContentType.Application.OctetStream)
            requestModifier()
            expectSuccess = false
            setBody(body)
        }.also(responseListener).let {
            ResultAndStatus(it.body(), it.status.value)
        }
    }
}

public fun RequestMaker(
    client: HttpClient,
    responseListener: (HttpResponse) -> Unit = {},
    requestModifier: HttpRequestBuilder.() -> Unit = {}
): RequestMaker =
    KtorClientRequestMaker(client, requestModifier, responseListener)