package com.rnett.krosstalk.client

import com.rnett.krosstalk.error.ExceptionEncoding
import com.rnett.krosstalk.error.KrosstalkResultStatus
import com.rnett.krosstalk.metadata.KrosstalkMethod
import com.rnett.krosstalk.metadata.KrosstalkSpec
import com.rnett.krosstalk.serialization.KrosstalkClientSerialization

public abstract class KrosstalkClient<T>(
    private val baseUrl: String,
    private val requestMaker: RequestMaker,
    private val serialization: KrosstalkClientSerialization,
    protected val spec: KrosstalkSpec<T>
) {

    init {
        serialization.initializeForSpec(spec)
    }

    protected suspend fun invoke(methodName: String, argumentValues: Map<String, Any?>): Any? {
        val method = spec.method(methodName)

        val body = serialization.serializeArguments(method, argumentValues)
        val url = baseUrl.trimEnd('/') + "/$methodName"
        val result = requestMaker.makeRequest(url, body)

        if (result.status == KrosstalkResultStatus.SUCCESS.statusCode) {
            return serialization.deserializeReturnValue(method, result.body)
        } else if (result.status in KrosstalkResultStatus.entries.map { it.statusCode }.toSet()) {
            ExceptionEncoding.tryDecode(result.body)?.let { throw it }
        }

        throw KrosstalkUnexpectedResponseException(method, result.status, result.body.decodeToString())
    }
}

public data class KrosstalkUnexpectedResponseException(
    val method: KrosstalkMethod,
    val status: Int,
    val response: String
) : RuntimeException(
    "Krosstalk call for ${method.methodFullyQualifiedName} received unexpected response with status $status: $response"
)