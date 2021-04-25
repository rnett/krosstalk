package com.rnett.krosstalk.pong

import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.ktor.server.KtorKrosstalkServer
import com.rnett.krosstalk.ktor.server.KtorServer
import com.rnett.krosstalk.ktor.server.defineKtor
import com.rnett.krosstalk.ping.ping
import com.rnett.krosstalk.serialization.KotlinxBinarySerializationHandler
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.CallLogging
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.cbor.Cbor
import org.slf4j.event.Level
import kotlin.system.exitProcess

internal actual object PongKrosstalk : Krosstalk(), KtorKrosstalkServer {
    actual override val serialization = KotlinxBinarySerializationHandler(Cbor { })
    override val server = KtorServer
}

fun startServer() {
    embeddedServer(CIO, 8084, "localhost") {

        install(CORS) {
            anyHost()
        }
        install(CallLogging) {
            level = Level.DEBUG
        }

        PongKrosstalk.defineKtor(this)
    }.start()
}

actual suspend fun pong(n: Int, call: Boolean): Boolean {
    println("Pong $n")
    if (n == 1000) {
        return false
    }
    if (call) {
        GlobalScope.launch {
            if (!ping(n + 1))
                exitProcess(0)
        }
    }
    return true
}