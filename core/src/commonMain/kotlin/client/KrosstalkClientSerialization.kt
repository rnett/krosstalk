package com.rnett.krosstalk.client

import com.rnett.krosstalk.metadata.KrosstalkSpec
import com.rnett.krosstalk.metadata.ResolvedMethod

public interface KrosstalkClientSerialization {

    /**
     * Optional method, called when a client is created for a given spec.
     * Gives the serializer a chance to do things like cache custom serializers for the spec's methods.
     */
    public fun initializeForSpec(spec: KrosstalkSpec<*>) {

    }

    public fun serializeArguments(method: ResolvedMethod, argumentValues: Map<String, Any?>): ByteArray
    public fun deserializeReturnValue(method: ResolvedMethod, data: ByteArray): Any?

}