package com.rnett.krosstalk.client

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

internal class KtorClientRequestMaker(private val client: HttpClient) : RequestMaker {
    override suspend fun makeRequest(url: String, body: ByteArray): ByteArray {
        return client.post(url) {
            setBody(body)
        }.body()
    }
}

public fun RequestMaker(client: HttpClient): RequestMaker = KtorClientRequestMaker(client)