package com.rnett.krosstalk.ktor.server

import com.rnett.krosstalk.*
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.util.pipeline.*

/**
 * Left should be reversed.  The last scope is applied first (as repeated last-removed are much faster than first-removed).
 */
fun wrapScopes(route: Route, left: MutableList<KtorServerScope>, final: Route.() -> Unit) {
    if (left.isEmpty())
        route.final()
    else {
        left.removeLast().apply {
            route.buildEndpoint {
                wrapScopes(this, left, final)
            }
        }
    }
}

object KtorServer : ServerHandler<KtorServerScope> {
    fun <K> define(app: Application, krosstalk: K) where K : Krosstalk, K : KrosstalkServer<KtorServerScope> {
        app.apply {
            krosstalk.methods.values
                .flatMap { krosstalk.neededServerScopes(it) }
                .distinct()
                .forEach {
                    it.apply {
                        configureApplication()
                    }
                }
        }

        app.routing {
            krosstalk.methods.forEach { (name, method) ->
                wrapScopes(this, krosstalk.neededServerScopes(method).toMutableList().asReversed()) {
                    route(
                        fillInEndpointWithStatic(method.endpoint, name, krosstalk.endpointPrefix),
                        HttpMethod(method.httpMethod)
                    ) {
                        handle {
                            val body = call.receiveChannel().toByteArray()
                            val response = krosstalk.handle(body)
                            call.respondBytes(response)
                        }
                    }
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

data class User(val username: String) : Principal

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