package com.rnett.krosstalk.client

public interface RequestMaker {
    /**
     * Should generally send an HTTP `POST` request to [url] with [body], with `application/octet-stream` content type and accept headers.
     */
    public suspend fun makeRequest(url: String, body: ByteArray): ByteArray
}