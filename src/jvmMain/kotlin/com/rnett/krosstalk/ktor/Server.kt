package com.rnett.krosstalk.ktor

import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.ServerHandler
import com.rnett.krosstalk.ServerScope
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.util.pipeline.*

@OptIn(ExperimentalStdlibApi::class)
fun buildRoutes(route: Route, left: MutableList<KtorServerScope>, final: Route.() -> Unit){
    if(left.isEmpty())
        route.final()
    else {
        left.removeFirst().apply {
            route.buildEndpoint {
                buildRoutes(this, left, final)
            }
        }
    }
}

actual object KtorServer : ServerHandler<KtorServerScope> {
    @OptIn(KtorExperimentalAPI::class)
    fun define(app: Application, krosstalk: Krosstalk<*, *, KtorServerScope>) {
        app.apply {
            krosstalk.methods.values
                .flatMap { it.requiredScopes }
                .map { it.server as KtorServerScope }
                .distinct()
                .forEach {
                    it.apply {
                        configureApplication()
                    }
                }
        }
        app.routing {

            route(krosstalk.endpointName) {

                krosstalk.methods.forEach { (name, method) ->
                    buildRoutes(this, method.requiredScopes.map { it.server as KtorServerScope }.toMutableList()){
                        post(name) {
                            val body = call.receiveChannel().toByteArray()
                            val response = krosstalk.handle(body)
                            call.respondBytes(response)
                        }
                    }
                }

                post("/" + krosstalk.endpointName) {
                    val body = call.receiveChannel().toByteArray()
                    val response = krosstalk.handle(body)
                    call.respondBytes(response)
                }
            }
        }
    }
}

actual interface KtorServerScope : ServerScope {
    fun Application.configureApplication() {}
    fun Route.buildEndpoint(block: Route.() -> Unit) {}
    fun PipelineContext<Unit, ApplicationCall>.handleRequest() {}
}

data class User(val username: String): Principal

actual class KtorServerAuth actual constructor(actual val accounts: Map<String, String>) : KtorServerScope {
    override fun Application.configureApplication() {
        install(Authentication){
            basic("krosstalk") {
                validate {
                    if(it.name in accounts && accounts[it.name] == it.password)
                        User(it.name)
                    else
                        null
                }
            }
        }
    }

    override fun Route.buildEndpoint(block: Route.() -> Unit) {
        authenticate("krosstalk") {
            block()
        }
    }
}