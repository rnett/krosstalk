package com.rnett.krosstalk.client

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
        return serialization.deserializeReturnValue(method, result)
    }
}