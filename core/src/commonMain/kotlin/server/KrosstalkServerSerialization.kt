package com.rnett.krosstalk.server

import com.rnett.krosstalk.metadata.ParameterType
import kotlin.reflect.KType

public interface KrosstalkServerSerialization {
    public fun deserialize(parameters: Map<String, ParameterType>, data: ByteArray): Map<String, Any?>

    public fun serialize(data: Any?, type: KType): ByteArray
}