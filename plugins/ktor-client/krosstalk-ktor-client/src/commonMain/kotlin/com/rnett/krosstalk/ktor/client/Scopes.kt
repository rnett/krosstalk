package com.rnett.krosstalk.ktor.client

import com.rnett.krosstalk.KrosstalkPluginApi
import com.rnett.krosstalk.client.plugin.ClientScope
import io.ktor.client.HttpClientConfig
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.HeadersBuilder


/**
 * A Ktor client scope.
 * Allows configuration of the client and of the request.
 * Note that a new client is used for each request.
 */
@OptIn(KrosstalkPluginApi::class)
public interface KtorClientScope<in D> : ClientScope<D> {
    public fun HttpClientConfig<*>.configureClient(data: D) {}
    public fun HttpRequestBuilder.configureRequest(data: D) {}
}

/**
 * A Ktor client scope that only adds headers.
 */
public fun interface KtorClientHeaderScope<D> : KtorClientScope<D> {
    override fun HttpRequestBuilder.configureRequest(data: D) {
        this.headers.headers(data)
    }

    public fun HeadersBuilder.headers(data: D): Unit
}