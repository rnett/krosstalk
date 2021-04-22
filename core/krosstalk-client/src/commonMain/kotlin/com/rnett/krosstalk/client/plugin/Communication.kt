package com.rnett.krosstalk.client.plugin

import com.rnett.krosstalk.Headers
import com.rnett.krosstalk.KrosstalkPluginApi


/**
 * The response to a Krosstalk request.
 */
@KrosstalkPluginApi
public class InternalKrosstalkResponse(
    public val statusCode: Int,
    public val headers: Headers,
    public val data: ByteArray,
    stringData: () -> String?,
) {
    public val stringData: String? by lazy {
        if (data.isEmpty())
            null
        else
            stringData()
    }

    @PublishedApi
    internal fun isSuccess(): Boolean = statusCode in 200..299
}

/**
 * A Krosstalk client handler.  Capable of sending krosstalk requests.
 */
@KrosstalkPluginApi
public interface ClientHandler<C : ClientScope<*>> {

    /**
     * Send a krosstalk request to the server.
     *
     * @param url The endpoint to send it to.
     * @param httpMethod The HTTP method (i.e. GET, POST) to use.
     * @param contentType the content type of the body
     * @param additionalHeaders additional headers to set on the request
     * @param body The body to send, or null if a body shouldn't be sent.
     * @param scopes The scopes to apply to the request.
     * @return The result of the request.
     */
    public suspend fun sendKrosstalkRequest(
        url: String,
        httpMethod: String,
        contentType: String,
        additionalHeaders: Headers,
        body: ByteArray?,
        scopes: List<AppliedClientScope<C, *>>,
    ): InternalKrosstalkResponse
}
