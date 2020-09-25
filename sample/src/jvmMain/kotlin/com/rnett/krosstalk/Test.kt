package com.rnett.krosstalk

import com.rnett.krosstalk.ktor.server.KtorServer
import com.rnett.krosstalk.ktor.server.KtorServerAuth
import com.rnett.krosstalk.ktor.server.KtorServerScope
import io.ktor.application.call
import io.ktor.application.install
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

actual object MyKrosstalk : Krosstalk(), KrosstalkServer<KtorServerScope>, Scopes {
    override val serialization = KotlinxBinarySerializationHandler(Cbor { })
    override val server = KtorServer
    override val auth by scope(KtorServerAuth(mapOf("username" to "password")))
}
