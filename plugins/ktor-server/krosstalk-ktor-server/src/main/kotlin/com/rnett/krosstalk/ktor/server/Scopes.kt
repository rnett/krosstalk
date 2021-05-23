package com.rnett.krosstalk.ktor.server

import com.rnett.krosstalk.KrosstalkPluginApi
import com.rnett.krosstalk.server.plugin.ServerScope
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.http.Headers
import io.ktor.routing.Route


/**
 * A Ktor server scope.  Supports configuring the application and wrapping endpoints.
 */
@OptIn(KrosstalkPluginApi::class)
public interface KtorServerScope<S : Any> : ServerScope<S> {
    /**
     * Configure the Ktor application.
     */
    public fun Application.configureApplication() {}

    /**
     * Wrap the endpoint.  **[endpoint] must be called at some point, it will build the rest of the endpoint.**
     *
     * @param optional Whether the scope is a optional scope for the method it is being applied to.
     * @param endpoint Builder for the rest of the endpoint.
     */
    public fun Route.wrapEndpoint(optional: Boolean, endpoint: Route.() -> Unit) {}

    /**
     * Get the scope's data.
     * Will throw an error if it returns null but the scope is non-optional
     *
     * TODO use a better way than nullability, I want to support nullable data
     */
    public fun getData(call: ApplicationCall): S?
}

/**
 * A Ktor server scope that only reads headers.
 */
public interface KtorServerHeaderScope<T : Any> : KtorServerScope<T> {
    public fun getValue(headers: Headers): T?

    override fun getData(call: ApplicationCall): T? {
        return getValue(call.request.headers)
    }
}
