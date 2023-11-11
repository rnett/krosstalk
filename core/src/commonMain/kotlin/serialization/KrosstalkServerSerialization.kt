package com.rnett.krosstalk.serialization

import com.rnett.krosstalk.metadata.KrosstalkMethod

public interface KrosstalkServerSerialization : KrosstalkSerialization {

    public fun deserializeArguments(method: KrosstalkMethod, data: ByteArray): Map<String, Any?>

    public fun serializeReturnValue(method: KrosstalkMethod, data: Any?): ByteArray
}