package com.rnett.krosstalk.client

import com.rnett.krosstalk.metadata.Argument
import com.rnett.krosstalk.metadata.KrosstalkSpec

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
        val types = spec.methods.getValue(methodName)
        val arguments = types.parameters
            .mapValues {
                Argument(it.value.type, argumentValues.getValue(it.key))
            }

        val body = serialization.serialize(arguments)
        val url = baseUrl.trimEnd('/') + "/$methodName"
        val result = requestMaker.makeRequest(url, body)
        return serialization.deserialize(result, types.returnType)
    }
}