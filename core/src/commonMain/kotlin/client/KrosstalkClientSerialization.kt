package com.rnett.krosstalk.client

import com.rnett.krosstalk.metadata.Argument
import kotlin.reflect.KType

public interface KrosstalkClientSerialization {

    public fun serialize(arguments: Map<String, Argument>): ByteArray
    public fun deserialize(data: ByteArray, type: KType): Any?

}