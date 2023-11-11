package com.rnett.krosstalk.client

public interface RequestMaker {
    public suspend fun makeRequest(url: String, body: ByteArray): ByteArray
}