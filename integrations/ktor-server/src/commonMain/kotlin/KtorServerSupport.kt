package com.rnett.krosstalk.server

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import io.ktor.util.pipeline.*

@KtorDsl
public fun Route.mount(
    server: KrosstalkServer<*>,
    extraHandler: PipelineContext<Unit, ApplicationCall>.() -> Unit = {}
) {
    server.mount { subPath, invoke ->
        post(subPath) {
            extraHandler()
            val body = call.receive<ByteArray>()
            val result = invoke(body)
            call.respondBytes(ContentType.Application.OctetStream, HttpStatusCode.fromValue(result.status.statusCode)) {
                result.data
            }
        }
    }
}

