package com.rnett.krosstalk.ping

import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.ktor.server.KtorKrosstalkServer
import com.rnett.krosstalk.ktor.server.KtorServer
import com.rnett.krosstalk.ktor.server.defineKtor
import com.rnett.krosstalk.pong.pong
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

internal actual object PingKrosstalk : Krosstalk(), KtorKrosstalkServer {
    actual override val serialization = KotlinxBinarySerializationHandler(Cbor { })
    override val server = KtorServer
}

fun startServer() {
    embeddedServer(CIO, 8083, "localhost") {

        install(CORS) {
            anyHost()
        }
        install(CallLogging) {
            level = Level.DEBUG
        }

        PingKrosstalk.defineKtor(this)
    }.start(true)
}

actual suspend fun ping(n: Int, call: Boolean): Boolean {
    println("Ping $n")
    if (n == 1000) {
        return false
    }
    if (call) {
        GlobalScope.launch {
            if (!pong(n + 1))
                exitProcess(0)
        }
    }
    return true
}