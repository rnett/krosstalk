package com.rnett.krosstalk.client

public interface RequestMaker {
    /**
     * Should generally send a HTTP `POST` request to [url] with [body], with `application/octet-stream` encoding.
     */
    public suspend fun makeRequest(url: String, body: ByteArray): ByteArray
}