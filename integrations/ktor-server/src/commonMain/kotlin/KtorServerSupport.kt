package com.rnett.krosstalk.server

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

public fun Routing.mount(server: KrosstalkServer<*>) {
    server.mount { subPath, invoke ->
        post(subPath) {
            val body = call.receive<ByteArray>()
            val result = invoke(body)
            call.response.status(HttpStatusCode.OK)
            call.respond(result)
        }
    }
}