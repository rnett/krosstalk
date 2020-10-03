package com.rnett.krosstalk.fullstack_sample

import com.rnett.krosstalk.KotlinxBinarySerializationHandler
import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.KrosstalkServer
import com.rnett.krosstalk.ktor.server.KtorServer
import com.rnett.krosstalk.ktor.server.KtorServerBasicAuth
import com.rnett.krosstalk.ktor.server.KtorServerScope
import com.rnett.krosstalk.scope
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.html.*
import kotlinx.serialization.cbor.Cbor

fun HTML.index() {
    head {
        title("Hello from Ktor!")
    }
    body {
        div {
            +"Hello from Ktor"
        }
        div {
            id = "root"
        }
        script(src = "/test.js") {}
    }
}

fun main() {
    embeddedServer(Netty, 8080, "localhost") {

        install(CORS) {
            anyHost()
        }
        KtorServer.define(this, MyKrosstalk)

        routing {

            get("/") {
                call.respondHtml(HttpStatusCode.OK, HTML::index)
            }

            static {
                resource("/test.js", "test.js")
                resource("/test.js.map", "test.js.map")
            }
        }

    }.start(true)
}

actual suspend fun doAuthThing(num: Int): Data {
    return Data(num, (num * 10).toString())
}

actual suspend fun doThing(data: Data): List<String> {
    return List(data.num) { data.str }
}

actual suspend fun Int.doExt(other: Int): Double = (this + other).toDouble()

data class User(val username: String) : Principal

private val validUsers = mapOf("username" to "password")

actual object MyKrosstalk : Krosstalk(), KrosstalkServer<KtorServerScope>, Scopes {
    override val serialization = KotlinxBinarySerializationHandler(Cbor { })
    override val server = KtorServer
    override val auth by scope(KtorServerBasicAuth {
        validate {
            if (validUsers[it.name] == it.password)
                User(it.name)
            else
                null
        }
    })
}
