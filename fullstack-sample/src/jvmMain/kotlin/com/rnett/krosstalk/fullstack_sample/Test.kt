package com.rnett.krosstalk.fullstack_sample

import com.rnett.krosstalk.*
import com.rnett.krosstalk.ktor.server.KtorServer
import com.rnett.krosstalk.ktor.server.KtorServerBasicAuth
import com.rnett.krosstalk.ktor.server.KtorServerScope
import com.rnett.krosstalk.serialization.KotlinxBinarySerializationHandler
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Principal
import io.ktor.features.CORS
import io.ktor.html.respondHtml
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.resource
import io.ktor.http.content.static
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.html.*
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.encodeToByteArray

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

actual suspend fun doThing(data: Data): List<String> {
    return List(data.num) { data.str }
}

actual suspend fun doEmptyThing(): Int = 5

actual suspend fun doAuthThing(num: Int): Data {
    return Data(num, (num * 10).toString())
}

actual suspend fun Int.doExt(other: Int): Double = (this + other).toDouble()

actual suspend fun doExplicitServerExceptionTest(): KrosstalkResult<Int> {
    throwException()
}

fun throwException(): Nothing {
    error("This is the expected error")
}

actual suspend fun doAuthHTTPExceptionTest(): KrosstalkResult<Int> {
    return KrosstalkResult.Success(2)
}

actual suspend fun doEndpointTest(data: Data, value: Int): String = List(value){data.toString()}.joinToString(" + ")

data class User(val username: String) : Principal

private val validUsers = mapOf("username" to "password")

actual object MyKrosstalk : Krosstalk(), KrosstalkServer<KtorServerScope>, Scopes {
    actual override val serialization = KotlinxBinarySerializationHandler(Cbor { })
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