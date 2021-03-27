package com.rnett.krosstalk.fullstack_sample

import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.KrosstalkResult
import com.rnett.krosstalk.KrosstalkServer
import com.rnett.krosstalk.Scope
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

actual suspend fun basicTest(data: Data): List<String> {
    return List(data.num) { data.str }
}

actual suspend fun basicEndpointTest(number: Int, str: String): String = str.repeat(number)

actual suspend fun endpointMethodTest(a: Int, b: Int): Int = a + b

actual suspend fun emptyGet(): String = "Hello World!"

actual suspend fun paramEndpointNoMinimize(a: Int, b: Int, c: Int, d: Int): Int = a * b * c * d

actual suspend fun optionalEndpointNoMinimize(n: Int, s: String?): String? = s?.repeat(n)

actual suspend fun optionalEndpointQueryParamsNoMinimize(n: Int, s: String?): String? = s?.repeat(n)

actual suspend fun paramEndpointMinimize(a: Int, b: Int, c: Int, d: Int): Int = a * b * c * d

actual suspend fun optionalEndpointMinimize(n: Int, s: String?): String? = s?.repeat(n)

actual suspend fun optionalEndpointQueryParamsMinimize(n: Int, s: String?): String? = s?.repeat(n)

actual suspend fun partialMinimize(n: Int, s: String?): String? = s?.repeat(n)

actual suspend fun withResult(n: Int): KrosstalkResult<Int> {
    if (n < 0)
        error("Can't have n < 0")

    return KrosstalkResult(n)
}