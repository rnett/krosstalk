package com.rnett.krosstalk.fullstack_sample

import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.KrosstalkResult
import com.rnett.krosstalk.KrosstalkServer
import com.rnett.krosstalk.Scope
import com.rnett.krosstalk.ScopeInstance
import com.rnett.krosstalk.ktor.server.KtorServer
import com.rnett.krosstalk.ktor.server.KtorServerAuth
import com.rnett.krosstalk.ktor.server.KtorServerScope
import com.rnett.krosstalk.serialization.KotlinxBinarySerializationHandler
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.Principal
import io.ktor.auth.basic
import io.ktor.features.CORS
import io.ktor.features.CallLogging
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import kotlinx.serialization.cbor.Cbor
import org.slf4j.event.Level


fun main() {
    embeddedServer(CIO, 8080, "localhost") {

        install(CORS) {
            anyHost()
        }
        install(CallLogging) {
            level = Level.DEBUG
        }

        KtorServer.define(this, MyKrosstalk)

    }.start(true)
}

actual suspend fun doThing(data: Data): List<String> {
    return List(data.num) { data.str }
}

actual suspend fun doEmptyThing(): Int = 5

actual suspend fun doAuthThing(num: Int, auth: ScopeInstance<MyKrosstalk.Auth>): Data {
    return Data(num, (num * 10).toString())
}

actual suspend fun Int.doExt(other: Int): Double = (this + other).toDouble()

actual suspend fun doExplicitServerExceptionTest(): KrosstalkResult<Int> {
    throwException()
}

fun throwException(): Nothing {
    error("This is the expected error")
}

actual suspend fun doAuthHTTPExceptionTest(auth: ScopeInstance<MyKrosstalk.Auth>): KrosstalkResult<Int> {
    return KrosstalkResult.Success(2)
}

actual suspend fun doEndpointTest(data: Data, value: Int): String = List(value){data.toString()}.joinToString(" + ")

data class User(val username: String) : Principal

private val validUsers = mapOf("username" to "password")

actual object MyKrosstalk : Krosstalk(), KrosstalkServer<KtorServerScope<*>> {
    actual override val serialization = KotlinxBinarySerializationHandler(Cbor { })
    override val server = KtorServer

    actual object Auth : Scope, KtorServerAuth("auth") {
        override fun Authentication.Configuration.configureAuth() {
            basic("auth") {
                validate {
                    if (validUsers[it.name] == it.password)
                        User(it.name)
                    else
                        null
                }
            }
        }
    }
}