package com.rnett.krosstalk.client

import com.rnett.krosstalk.InternalKrosstalkApi
import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.KrosstalkException
import com.rnett.krosstalk.KrosstalkPluginApi
import com.rnett.krosstalk.KrosstalkResult
import com.rnett.krosstalk.httpEncode
import com.rnett.krosstalk.serialization.getMethodSerializer


/**
 * The response to a Krosstalk request.
 */
@KrosstalkPluginApi
class InternalKrosstalkResponse(val statusCode: Int, val data: ByteArray, stringData: () -> String?) {
    val stringData by lazy {
        if (data.isEmpty())
            null
        else
            stringData()
    }

    @PublishedApi
    internal fun isSuccess() = statusCode in 200..299
}

/**
 * A Krosstalk client handler.  Capable of sending krosstalk requests.
 */
@KrosstalkPluginApi
interface ClientHandler<C : ClientScope<*>> {

    /**
     * The url of the server to send requests to.
     */
    val serverUrl: String


    /**
     * Helper function to concatenate the server url and the endpoint
     */
    fun requestUrl(endpoint: String) = "${serverUrl.trimEnd('/')}/${endpoint.trimStart('/')}"

    /**
     * Send a krosstalk request to the server.
     * Should generally make a [httpMethod] request to `$serverUrl/$endpoint` (which can be gotten using [requestUrl]), but can change this if necessary.
     *
     * @param endpoint The endpoint to send it to.
     * @param httpMethod The HTTP method (i.e. GET, POST) to use.
     * @param body The body to send, or null if a body shouldn't be sent.
     * @param scopes The scopes to apply to the request.
     * @return The result of the request.
     */
    suspend fun sendKrosstalkRequest(
        endpoint: String,
        httpMethod: String,
        contentType: String,
        body: ByteArray?,
        scopes: List<AppliedClientScope<C, *>>,
    ): InternalKrosstalkResponse

    fun getStatusCodeName(httpStatusCode: Int): String?
}


//fun ServerHandler<*>.fillEndpoint(endpointTemplate: String, methodName: String, )

/**
 * Placeholder for a Krosstalk client side method.  Will be replaced by the compiler plugin.
 */
@OptIn(InternalKrosstalkApi::class)
suspend fun krosstalkCall(): Nothing =
    throw KrosstalkException.CompilerError("Should have been replaced with a krosstalk call during compilation")


@OptIn(InternalKrosstalkApi::class, KrosstalkPluginApi::class, ExperimentalStdlibApi::class)
@Suppress("unused")
@PublishedApi
internal suspend inline fun <T, K, reified C : ClientScope<*>> K.call(
    methodName: String,
    arguments: Map<String, *>,
    scopes: List<AppliedClientScope<C, *>>,
): T
        where K : KrosstalkClient<C>, K : Krosstalk {
    val method = requiredMethod(methodName)

    val bodyArguments = method.bodyArguments(arguments)
    println("Body Arguments: ${bodyArguments.keys}")
    val serializedBody = method.serializers.transformedParamSerializers.serializeArgumentsToBytes(bodyArguments)

    val serializedUrlArgs = method.urlArguments(arguments)
        .mapValues {
            method.serializers.transformedParamSerializers.serializeArgumentToString(it.key, it.value).httpEncode()
        }


    val result = client.sendKrosstalkRequest(
        method.endpoint.fillWithArgs(
            methodName,
            serializedUrlArgs,
            arguments.filter { it.value != null }.keys
        ),
        method.httpMethod,
        method.contentType ?: serialization.contentType,
        if (!method.hasBodyArguments(arguments)) null else serializedBody,
        scopes
    )

    return if (method.useExplicitResult) {
        if (result.isSuccess()) {
            KrosstalkResult.Success(method.serializers.transformedResultSerializer.deserializeFromBytes(
                result.data
            )) as T
        } else {
            if (result.statusCode == 500) {
                try {
                    serialization.getMethodSerializer<KrosstalkResult.ServerException>().deserializeFromBytes(result.data) as T
                } catch (t: Throwable) {
                    KrosstalkResult.HttpError(result.statusCode, client.getStatusCodeName(result.statusCode), result.stringData) as T
                }
            } else {
                KrosstalkResult.HttpError(result.statusCode, client.getStatusCodeName(result.statusCode), result.stringData) as T
            }
        }
    } else {
        if (result.isSuccess()) {
            method.serializers.transformedResultSerializer.deserializeFromBytes(
                result.data
            ) as T
        } else {
            throw KrosstalkException.CallFailure(methodName, result.statusCode, client.getStatusCodeName(result.statusCode), result.stringData)
        }
    }
}
