package com.rnett.krosstalk.client_sample

import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.ContentNegotiation
import io.ktor.html.respondHtml
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.resource
import io.ktor.http.content.static
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.serialization.json
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.html.*
import kotlinx.serialization.json.Json

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

val knownItems = List(20) { Item(it, "Item $it") }.associateBy { it.id }

fun main() {
    embeddedServer(Netty, 8080, "localhost") {

        install(CORS) {
            anyHost()
        }

        install(ContentNegotiation) {
            json(Json { })
        }

        routing {

            get("/") {
                call.respondHtml(HttpStatusCode.OK, HTML::index)
            }

            route("items") {
                get("{id}") {
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            "id must be an Int"
                    )

                    knownItems[id]?.let {
                        call.respond(it)
                    } ?: call.respond(HttpStatusCode.NotFound, "No item with id $id")
                }
                get {
                    call.respond(knownItems.values.map { it.id })
                }
            }

            static {
                resource("/test.js", "test.js")
                resource("/test.js.map", "test.js.map")
            }
        }

    }.start(true)
}
