package com.rnett.krosstalk.client_test

import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.Principal
import io.ktor.auth.authenticate
import io.ktor.auth.authentication
import io.ktor.auth.basic
import io.ktor.features.CORS
import io.ktor.features.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.resource
import io.ktor.http.content.static
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.serialization.json
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val knownItems = List(20) { Item(it, "Item $it") }.associateBy { it.id }

data class User(val username: String) : Principal

val users = mapOf("user" to "pass")

fun main() {
    embeddedServer(CIO, 8080, "localhost") {

        install(CORS) {
            anyHost()
        }

        install(ContentNegotiation) {
            json(Json { })
        }

        install(Authentication) {
            basic {
                validate {
                    if (users[it.name] == it.password)
                        User(it.name)
                    else
                        null
                }
            }
        }

        routing {

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

            authenticate {
                get("user") {
                    call.respond(Json { }.encodeToString(call.authentication.principal<User>()!!.username))
                }
            }

            route("inner/server/path") {
                get("test") {
                    call.respond(HttpStatusCode.OK)
                }
            }



            static {
                resource("/test.js", "test.js")
                resource("/test.js.map", "test.js.map")
            }
        }

    }.start(true)
}
