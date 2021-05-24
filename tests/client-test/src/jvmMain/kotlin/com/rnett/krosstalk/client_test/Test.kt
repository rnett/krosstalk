package com.rnett.krosstalk.client_test

import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.*
import io.ktor.features.CORS
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.content.resource
import io.ktor.http.content.static
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.shared.serialization.ContentConverter
import io.ktor.util.getOrFail
import io.ktor.util.reflect.TypeInfo
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.core.readText
import io.ktor.utils.io.readRemaining
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

val knownItems = List(20) { Item(it, "Item $it") }.associateBy { it.id }

data class User(val username: String) : Principal

val users = mapOf("user" to "pass")

fun TypeInfo.serializer(): KSerializer<Any> = kotlinType?.let { serializer(it) as KSerializer<Any> } ?: serializer(reifiedType)

fun main() {
    embeddedServer(CIO, 8081, "localhost") {
        val json = Json {  }

        install(CORS) {
            anyHost()
        }

        install(ContentNegotiation) {

            register(ContentType.Application.Json, object : ContentConverter{
                override suspend fun deserialize(charset: Charset, typeInfo: TypeInfo, content: ByteReadChannel): Any? {
                    return json.decodeFromString(typeInfo.serializer(), content.readRemaining().readText(charset))
                }

                override suspend fun serialize(contentType: ContentType, charset: Charset, typeInfo: TypeInfo, value: Any): OutgoingContent? {
                    return TextContent(json.encodeToString(typeInfo.serializer(), value), contentType)
                }
            })
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
                    val id = call.parameters.getOrFail("id").toIntOrNull() ?: return@get call.respond(
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
