package com.rnett.krosstalk

import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.request.*

actual interface ServerHandler<S: ServerScope>

actual interface ClientHandler<C: ClientScope>{
    suspend fun sendKrosstalkRequest(methodName: String, body: ByteArray, scopes: List<C>): ByteArray
}