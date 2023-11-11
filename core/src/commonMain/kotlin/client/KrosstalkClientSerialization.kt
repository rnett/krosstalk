package com.rnett.krosstalk.client

import com.rnett.krosstalk.metadata.Argument
import com.rnett.krosstalk.metadata.KrosstalkSpec
import kotlin.reflect.KType

public interface KrosstalkClientSerialization {

    /**
     * Optional method, called when a client is created for a given spec.
     * Gives the serializer a chance to do things like cache custom serializers for the spec's methods.
     */
    public fun initializeForSpec(spec: KrosstalkSpec<*>) {

    }

    public fun serialize(arguments: Map<String, Argument>): ByteArray
    public fun deserialize(data: ByteArray, type: KType): Any?

}