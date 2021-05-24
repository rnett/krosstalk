package com.rnett.krosstalk.native_test

import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.Scope
import com.rnett.krosstalk.ktor.server.KtorServer
import com.rnett.krosstalk.ktor.server.KtorServerScope
import com.rnett.krosstalk.ktor.server.auth.KtorServerBasicAuth
import com.rnett.krosstalk.ktor.server.defineKtor
import com.rnett.krosstalk.serialization.KotlinxBinarySerializationHandler
import com.rnett.krosstalk.server.KrosstalkServer
import io.ktor.application.install
import io.ktor.auth.BasicAuthenticationProvider
import io.ktor.auth.Principal
import io.ktor.features.CORS
import io.ktor.features.CallLogging
import io.ktor.routing.routing
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import kotlinx.serialization.cbor.Cbor
import org.slf4j.event.Level

fun main() {
    embeddedServer(CIO, 8087, "localhost") {

        install(CORS) {
            anyHost()
        }
        install(CallLogging) {
            level = Level.DEBUG
        }

        routing {
//            trace { application.log.info(it.buildText()) }
            MyKrosstalk.defineKtor(this)
        }

    }.start(true)
}

data class User(val username: String) : Principal

private val validUsers = mapOf("username" to "password")

actual object MyKrosstalk : Krosstalk(), KrosstalkServer<KtorServerScope<*>> {
    actual override val serialization = KotlinxBinarySerializationHandler(Cbor { })
    override val server = KtorServer

    actual object Auth : Scope, KtorServerBasicAuth<User>("auth") {
        override fun BasicAuthenticationProvider.Configuration.configure() {
            validate {
                if (validUsers[it.name] == it.password)
                    User(it.name)
                else
                    null
            }
        }
    }
}

actual suspend fun testBasic(id: Int, name: String): Item = Item(id, name)