package com.rnett.krosstalk.ktor

import com.rnett.krosstalk.*
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.Principal
import io.ktor.auth.authenticate
import io.ktor.auth.basic
import io.ktor.request.receiveChannel
import io.ktor.response.respondBytes
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.pipeline.PipelineContext
import io.ktor.util.toByteArray

/**
 * Left should be reversed.  The last scope is applied first (as repeated last-removed are much faster than first-removed).
 */
@OptIn(ExperimentalStdlibApi::class)
fun buildRoutes(route: Route, left: MutableList<KtorServerScope>, final: Route.() -> Unit){
    if(left.isEmpty())
        route.final()
    else {
        left.removeLast().apply {
            route.buildEndpoint {
                buildRoutes(this, left, final)
            }
        }
    }
}

object KtorServer : ServerHandler<KtorServerScope> {
    @OptIn(KtorExperimentalAPI::class)
    fun <K> define(app: Application, krosstalk: K) where K : Krosstalk, K : KrosstalkServer<KtorServerScope> {
        app.apply {
            krosstalk.methods.values
                    .flatMap { krosstalk.requiredServerScopes(it) }
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
                    buildRoutes(this, krosstalk.requiredServerScopes(method).toMutableList().asReversed()) {
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

interface KtorServerScope : ServerScope {
    fun Application.configureApplication() {}
    fun Route.buildEndpoint(block: Route.() -> Unit) {}
    fun PipelineContext<Unit, ApplicationCall>.handleRequest() {}
}

data class User(val username: String): Principal

class KtorServerAuth(val accounts: Map<String, String>) : KtorServerScope {
    override fun Application.configureApplication() {
        install(Authentication) {
            basic("krosstalk") {
                validate {
                    if (it.name in accounts && accounts[it.name] == it.password)
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