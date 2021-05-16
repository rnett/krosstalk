package com.rnett.krosstalk.pong

import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.ktor.server.KtorKrosstalkServer
import com.rnett.krosstalk.ktor.server.KtorServer
import com.rnett.krosstalk.ktor.server.defineKtor
import com.rnett.krosstalk.ping.ping
import com.rnett.krosstalk.serialization.KotlinxBinarySerializationHandler
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.cbor.Cbor
import kotlin.system.exitProcess

internal actual object PongKrosstalk : Krosstalk(), KtorKrosstalkServer {
    actual override val serialization = KotlinxBinarySerializationHandler(Cbor { })
    override val server = KtorServer
}

fun startServer() {
    embeddedServer(CIO, 8085, "localhost") {
        install(CORS) {
            anyHost()
        }

        PongKrosstalk.defineKtor(this)
    }.start(true)
}

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        startServer()
    }
}

object TestServer {
    @JvmStatic
    fun main(args: Array<String>) {
        startServer()
    }
}

actual suspend fun pong(n: Int, call: Boolean): Boolean {
    println("Pong $n")
    if (n == 1000) {
        GlobalScope.launch {
            delay(1000)
            exitProcess(0)
        }
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