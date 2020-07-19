package com.rnett.krosstalk

import com.rnett.krosstalk.ktor.KtorServer
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.jetty.*
import kotlinx.html.*
import javax.management.Query.div

actual suspend fun doThing(data: Data): List<String> {
    return List(data.num) { data.str }
}

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
        script(src = "/krosstalk.js") {}
    }
}

fun main() {
    embeddedServer(Jetty, 8080, "localhost") {

        install(CORS){
            anyHost()
        }
        KtorServer.define(this, MyKrosstalk)

        routing {

            get("/") {
                call.respondHtml(HttpStatusCode.OK, HTML::index)
            }

            static {
                resource("/krosstalk.js", "krosstalk.js")
                resource("/krosstalk.js.map", "krosstalk.js.map")
            }
        }

    }.start(true)
}

actual suspend fun doAuthThing(num: Int): Data {
    return Data(num, (num * 10).toString())
}