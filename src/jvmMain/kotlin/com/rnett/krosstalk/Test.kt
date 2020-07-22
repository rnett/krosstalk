package com.rnett.krosstalk

import com.rnett.krosstalk.ktor.KtorServer
import com.rnett.krosstalk.ktor.KtorServerAuth
import com.rnett.krosstalk.ktor.KtorServerScope
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
import io.ktor.server.jetty.Jetty
import kotlinx.html.*
import kotlinx.serialization.builtins.list
import kotlinx.serialization.builtins.serializer

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

actual suspend fun doThing(data: Data): List<String> {
    return List(data.num) { data.str }
}

actual object MyKrosstalk : Krosstalk<KotlinxSerializers>(), KrosstalkServer<KtorServerScope>, Scopes {
    override val serialization = KotlinxSerializationHandler
    override val server = KtorServer
    override val auth by scope(KtorServerAuth(mapOf("username" to "password")))

    //TODO registering methods should be handled by compiler plugin
    init {
        addMethod("doThing", ::doThing, KotlinxSerializers(mapOf("data" to Data.serializer()), String.serializer().list))
        addMethod("doAuthThing", ::doAuthThing, KotlinxSerializers(mapOf("num" to Int.serializer()), Data.serializer()), "auth")
    }
}