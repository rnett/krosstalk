package com.rnett.krosstalk.serialization

import com.rnett.krosstalk.KrosstalkPluginApi


/**
 * A serializer/deserializer that transforms [T] to and from [S] and visa versa, and transforms [S] to and from [ByteArray].
 */
@KrosstalkPluginApi
public interface Serializer<T, S> {

    /**
     * Deserialize [T] from [S].
     */
    public fun deserialize(data: S): T

    /**
     * Serialize [T] to [S].
     */
    public fun serialize(data: T): S
}

/**
 * A serializer that serializes/deserializes to/from [String].
 */
@KrosstalkPluginApi
public typealias StringSerializer<T> = Serializer<T, String>


/**
 * A serializer that directly serializes/deserializes to/from [ByteArray].
 */
@KrosstalkPluginApi
public typealias BinarySerializer<T> = Serializer<T, ByteArray>
