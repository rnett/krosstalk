package com.rnett.krosstalk.client_test

import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.*
import io.ktor.features.CORS
import io.ktor.features.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.resource
import io.ktor.http.content.static
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.serialization.json
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.util.getOrFail
import io.ktor.util.reflect.TypeInfo
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.util.concurrent.ConcurrentHashMap

private val knownItems = List(20) { Item(it, "Item $it") }.associateBy { it.id }.toMap(ConcurrentHashMap())

data class User(val username: String) : Principal

val users = mapOf("user" to "pass")

fun TypeInfo.serializer(): KSerializer<Any> =
    kotlinType?.let { serializer(it) as KSerializer<Any> } ?: serializer(reifiedType)

fun main() {
    embeddedServer(CIO, 8081, "localhost") {
        install(CORS) {
            anyHost()
        }

        install(ContentNegotiation) {
            json()
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
                route("{id}") {
                    get {
                        val id = call.parameters.getOrFail("id").toIntOrNull() ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            "id must be an Int"
                        )

                        knownItems[id]?.let {
                            call.respond(it)
                        } ?: call.respond(HttpStatusCode.NotFound, "No item with id $id")
                    }
                }
                get {
                    call.respond(knownItems.values.map { it.id })
                }
                post {
                    val item = call.receive<ItemSetRequest>()
                    knownItems[item.id] = item.item
                    call.respond(HttpStatusCode.OK)
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

            get("testBearer") {
                val bearer: String = call.request.headers["Authorization"] ?: run {
                    call.respond(HttpStatusCode.Unauthorized, "No authentication")
                    error("No authentication")
                }

                if (bearer != "Bearer testToken") {
                    call.respond(HttpStatusCode.Forbidden, "Bad authentication")
                } else {
                    call.respond(10)
                }
            }

            static {
                resource("/test.js", "test.js")
                resource("/test.js.map", "test.js.map")
            }
        }

    }.start(true)
}
