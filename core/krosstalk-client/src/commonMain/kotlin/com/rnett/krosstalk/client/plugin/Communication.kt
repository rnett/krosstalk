package com.rnett.krosstalk.client.plugin

import com.rnett.krosstalk.Headers
import com.rnett.krosstalk.KrosstalkPluginApi


/**
 * The response to a Krosstalk request.
 *
 * @property statusCode the http status code of the response
 * @property headers the response headers
 * @property body the response body
 * @param stringBody a lambda to get the body as a string, or null if not possible.  Will likely use the response's charset header
 * @property stringBody the body as a string, or null if not possible or the body is empty.  Calculated using the `stringBody` parameter.
 */
@KrosstalkPluginApi
public class InternalKrosstalkResponse(
    public val statusCode: Int,
    public val headers: Headers,
    public val body: ByteArray,
    stringBody: () -> String?,
) {
    public val stringBody: String? by lazy {
        if (body.isEmpty())
            null
        else
            stringBody()
    }

    @PublishedApi
    internal fun isSuccess(): Boolean = statusCode in 200..299
}

/**
 * A Krosstalk client handler.  Capable of sending krosstalk requests.
 */
//TODO needs to be implementing only
//@KrosstalkPluginApi
@OptIn(KrosstalkPluginApi::class)
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
    @OptIn(KrosstalkPluginApi::class)
    public suspend fun sendKrosstalkRequest(
        url: String,
        httpMethod: String,
        contentType: String,
        additionalHeaders: Headers,
        body: ByteArray?,
        scopes: List<AppliedClientScope<C, *>>,
    ): InternalKrosstalkResponse
}
