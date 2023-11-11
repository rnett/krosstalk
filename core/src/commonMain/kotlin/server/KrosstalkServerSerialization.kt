package com.rnett.krosstalk.server

import com.rnett.krosstalk.metadata.KrosstalkSpec
import com.rnett.krosstalk.metadata.ResolvedMethod

public interface KrosstalkServerSerialization {

    /**
     * Optional method, called when a server is created for a given spec.
     * Gives the serializer a chance to do things like cache custom serializers for the spec's methods.
     */
    public fun initializeForSpec(spec: KrosstalkSpec<*>) {

    }

    public fun deserializeArguments(method: ResolvedMethod, data: ByteArray): Map<String, Any?>

    public fun serializeReturnValue(method: ResolvedMethod, data: Any?): ByteArray
}