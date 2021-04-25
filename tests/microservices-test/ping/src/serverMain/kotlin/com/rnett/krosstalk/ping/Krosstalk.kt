package com.rnett.krosstalk.ping

import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.client.ClientFailureException
import com.rnett.krosstalk.ktor.server.KtorKrosstalkServer
import com.rnett.krosstalk.ktor.server.KtorServer
import com.rnett.krosstalk.ktor.server.defineKtor
import com.rnett.krosstalk.pong.pong
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

internal actual object PingKrosstalk : Krosstalk(), KtorKrosstalkServer {
    actual override val serialization = KotlinxBinarySerializationHandler(Cbor { })
    override val server = KtorServer
}

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        GlobalScope.launch {
            delay(2000)
            startPing()
        }
        startServer()
    }
}

object TestServer {
    @JvmStatic
    fun main(args: Array<String>) {
        startServer()
    }
}

fun startServer() {
    embeddedServer(CIO, 8083, "localhost") {
        install(CORS) {
            anyHost()
        }

        PingKrosstalk.defineKtor(this)
    }.start(true)
}

suspend fun startPing() {
    println("Ping 0")
    for (i in 1..1000) {
        try {
            if (!pong(1))
                exitProcess(0)
            break
        } catch (e: ClientFailureException) {
            if (e.cause.message?.contains("Connection refused") != true)
                break
            println("Connection $i failed")
        }
    }
}

actual suspend fun ping(n: Int, call: Boolean): Boolean {
    println("Ping $n")
    if (n == 1000) {
        GlobalScope.launch {
            delay(1000)
            exitProcess(0)
        }
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