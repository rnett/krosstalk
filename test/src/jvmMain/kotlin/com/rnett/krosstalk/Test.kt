package com.rnett.krosstalk

import com.rnett.krosstalk.annotations.KrosstalkHost
import com.rnett.krosstalk.ktor.server.KtorServer
import com.rnett.krosstalk.ktor.server.KtorServerAuth
import com.rnett.krosstalk.ktor.server.KtorServerScope
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.jetty.*
import kotlinx.html.*
import kotlin.reflect.typeOf

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

actual suspend fun Int.doExt(other: Int): Double = (this + other).toDouble()

@OptIn(ExperimentalStdlibApi::class)
@KrosstalkHost
actual object MyKrosstalk : Krosstalk(), KrosstalkServer<KtorServerScope>, Scopes {
    override val serialization = KotlinxSerializationHandler
    override val server = KtorServer
    override val auth by scope(KtorServerAuth(mapOf("username" to "password")))

    //TODO registering methods should be handled by compiler plugin
    init {
        addMethod("doThing", ::doThing, MethodTypes(mapOf("data" to typeOf<Data>()), typeOf<List<String>>())) {}
        addMethod("doAuthThing", ::doAuthThing, MethodTypes(mapOf("num" to typeOf<Int>()), typeOf<Data>()), "auth") {}
        addMethod(
            "doExt",
            Int::doExt,
            MethodTypes(
                mapOf("other" to typeOf<Int>()),
                resultType = typeOf<Double>(),
                extensionReceiverType = typeOf<Int>()
            )
        ) {}
    }
}