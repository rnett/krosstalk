package com.rnett.krosstalk.serialization

import com.rnett.krosstalk.metadata.KrosstalkSpec

public interface KrosstalkSerialization {

    /**
     * Called when a client or server is created for a given spec.
     * Gives the serializer a chance to do things like cache custom serializers for the spec's methods.
     * Guaranteed to be called before any serialization or deserialization methods for methods of that spec.
     */
    public fun initializeForSpec(spec: KrosstalkSpec<*>) {

    }

    public companion object
}