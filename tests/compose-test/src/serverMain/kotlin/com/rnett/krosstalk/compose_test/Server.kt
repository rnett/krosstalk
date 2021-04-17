package com.rnett.krosstalk.compose_test

import com.rnett.krosstalk.ktor.server.defineKtor
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.CallLogging
import io.ktor.routing.routing
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import org.slf4j.event.Level

fun main() {
    println(TodoKrosstalk.methods)
    embeddedServer(CIO, 8080, "localhost") {
        install(CORS) {
            anyHost()
        }
        install(CallLogging) {
            level = Level.DEBUG
        }

        routing {
            TodoKrosstalk.defineKtor(this)
        }

    }.start(true)
}