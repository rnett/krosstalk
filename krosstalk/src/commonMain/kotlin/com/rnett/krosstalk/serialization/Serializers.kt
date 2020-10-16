package com.rnett.krosstalk.serialization


/**
 * A serializer/deserializer that transforms [T] to and from [S] and visa versa, and transforms [S] to and from [ByteArray].
 */
interface Serializer<T, S> {

    /**
     * Deserialize [T] from [S].
     */
    fun deserialize(data: S): T

    /**
     * Serialize [T] to [S].
     */
    fun serialize(data: T): S
}

/**
 * A serializer that serializes/deserializes to/from [String].
 */
interface StringSerializer<T> : Serializer<T, String>


/**
 * A serializer that directly serializes/deserializes to/from [ByteArray].
 */
interface BinarySerializer<T> : Serializer<T, ByteArray>
