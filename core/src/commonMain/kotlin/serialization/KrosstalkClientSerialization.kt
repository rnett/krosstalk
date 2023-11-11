package com.rnett.krosstalk.serialization

import com.rnett.krosstalk.metadata.KrosstalkMethod

public interface KrosstalkClientSerialization : KrosstalkSerialization {

    public fun serializeArguments(method: KrosstalkMethod, argumentValues: Map<String, Any?>): ByteArray
    public fun deserializeReturnValue(method: KrosstalkMethod, data: ByteArray): Any?

}