package com.rnett.krosstalk

import com.rnett.krosstalk.serialization.*

/**
 * The response to a Krosstalk request.
 */
sealed class InternalKrosstalkResponse {
    /**
     * The HTTP response code.
     */
    abstract val responseCode: Int

    /**
     * A Successful request, returning the serialized return value.
     */
    data class Success(override val responseCode: Int, val data: ByteArray) : InternalKrosstalkResponse() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Success) return false

            if (responseCode != other.responseCode) return false
            if (!data.contentEquals(other.data)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = responseCode
            result = 31 * result + data.contentHashCode()
            return result
        }
    }

    /**
     * A failed request.  Can throw a custom error using [error], will throw [KrosstalkException.CallFailure] if [error] is null.
     */
    data class Failure(override val responseCode: Int, val error: (suspend (methodName: String) -> Nothing)? = null) :
        InternalKrosstalkResponse()
}

/**
 * A Krosstalk client handler.  Capable of sending krosstalk requests.
 */
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
     * Helper function to throw a [KrosstalkException.CallFailure].
     */
    fun callFailedException(
        methodName: String,
        httpStatusCode: Int,
        message: String = "Krosstalk method $methodName failed with HTTP status code $httpStatusCode"
    ): Nothing = throw KrosstalkException.CallFailure(methodName, httpStatusCode, message)

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
        body: ByteArray?,
        scopes: List<AppliedClientScope<C, *>>,
    ): InternalKrosstalkResponse
}

/**
 * A Krosstalk server handler.
 * Should help you set up receiver endpoints for all Krosstalk methods.
 * No behavior is defined here as what is necessary to do that, and where and how you will want to call it, will vary widely depending on which server you are using.
 *
 * Will almost certainly want to use [fillWithStaticAndAdjustParameters], [fillWithStatic], or [splitEndpoint] to handle endpoint templates.
 */
interface ServerHandler<S : ServerScope<*>>

//fun ServerHandler<*>.fillEndpoint(endpointTemplate: String, methodName: String, )

/**
 * Placeholder for a Krosstalk client side method.  Will be replaced by the compiler plugin.
 */
suspend fun krosstalkCall(): Nothing =
    throw KrosstalkException.CompilerError("Should have been replaced with a krosstalk call during compilation")

const val baseUrlLegalRegex = "-a-zA-Z0-9._*~'()!"

@OptIn(ExperimentalUnsignedTypes::class)
fun String.httpEncode(legal: String = baseUrlLegalRegex): String {
    val regex = Regex("[^$legal]")
    return this.replace(regex) {
        "%" + it.value[0].toByte().toUByte().toString(16).toUpperCase().let {
            if (it.length == 1)
                "0$it"
            else
                it
        }
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
fun String.httpDecode(): String {
    return Regex("%([0-9A-F]{2})").replace(this) {
        it.groupValues[1].toUByte(16).toByte().toChar().toString()
    }
}


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
        ), //TODO fix
        method.httpMethod,
        if (!method.hasBodyArguments(arguments)) null else serializedBody,
        scopes
    )

    @Suppress("UNCHECKED_CAST") // checked in compiler plugin
    if (result.responseCode in method.nullOnResponseCodes)
        return null as T

    if (method.useExplicitResult) {
        //TODO ensure T is KrosstalkResult (is done in compiler plugin too, but wouldn't hurt)
        return when (result) {
            // will be a success or exception
            is InternalKrosstalkResponse.Success -> method.serializers.transformedResultSerializer.deserializeFromBytes(
                result.data
            ) as T
            is InternalKrosstalkResponse.Failure -> {
                try {
                    result.error?.let { it(methodName) }
                        ?: client.callFailedException(methodName, result.responseCode)
                } catch (e: Throwable) {
                    KrosstalkResult.failure(KrosstalkFailure.HttpError(result.responseCode, e.message))
                } as T
            }
        }
    } else {
        return when (result) {
            is InternalKrosstalkResponse.Success -> method.serializers.transformedResultSerializer.deserializeFromBytes(
                result.data
            ) as T
            is InternalKrosstalkResponse.Failure -> result.error?.let { it(methodName) }
                ?: client.callFailedException(methodName, result.responseCode)
        }
    }
}

/**
 * Helper method for server side to handle a method [methodName] with the body data [data].
 *
 * @return The data that should be sent as a response.
 */
suspend fun <K> K.handle(
    methodName: String,
    urlArguments: Map<String, String>,
    data: ByteArray,
    scopes: WantedScopes,
): ByteArray where K : Krosstalk, K : KrosstalkServer<*> {
    val wantedScopes = scopes.toImmutable()
    val method = requiredMethod(methodName)

    val arguments = if (data.isNotEmpty())
        method.serializers.transformedParamSerializers.deserializeArgumentsFromBytes(data).toMutableMap()
    else
        mutableMapOf()

    if (method.minimizeBody) {
        val endpointArguments: Map<String, Any?> = urlArguments.mapValues {
            method.serializers.transformedParamSerializers.deserializeArgumentFromString(it.key, it.value.httpDecode())
        }

        arguments += endpointArguments
    }

    val result = if (method.useExplicitResult) {
        try {
            method.call(arguments, wantedScopes)
        } catch (e: Throwable) {
            KrosstalkResult.failure(KrosstalkFailure.ServerException(ExceptionData(e, method.includeStacktrace)))
        }
    } else {
        method.call(arguments, wantedScopes)
    }

    return method.serializers.transformedResultSerializer.serializeToBytes(result)
}