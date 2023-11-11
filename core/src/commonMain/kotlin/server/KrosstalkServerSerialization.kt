package com.rnett.krosstalk.server

import com.rnett.krosstalk.metadata.KrosstalkSpec
import com.rnett.krosstalk.metadata.ParameterType
import kotlin.reflect.KType

public interface KrosstalkServerSerialization {

    /**
     * Optional method, called when a server is created for a given spec.
     * Gives the serializer a chance to do things like cache custom serializers for the spec's methods.
     */
    public fun initializeForSpec(spec: KrosstalkSpec<*>) {

    }

    public fun deserialize(parameters: Map<String, ParameterType>, data: ByteArray): Map<String, Any?>

    public fun serialize(data: Any?, type: KType): ByteArray
}