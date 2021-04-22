package com.rnett.krosstalk.serialization.plugin

import com.rnett.krosstalk.InternalKrosstalkApi
import com.rnett.krosstalk.KrosstalkPluginApi
import com.rnett.krosstalk.serialization.MissingSerializerException


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


/**
 * A `{Argument -> Serializer}` map, with helper functions to get the needed serializer as `Serializer<Any?, S>` rather than `Serializer<*, S>` and to serialize/deserialize all arguments.
 */
@KrosstalkPluginApi
public class ArgumentSerializers<S>(public val map: Map<String, Serializer<*, S>>) {
    @KrosstalkPluginApi
    public operator fun contains(argument: String): Boolean = argument in map

    /**
     * Get a serializer for an argument as a `Serializer<Any?, S>`, throwing [MissingSerializerException] if a serializer is missing.
     */
    @OptIn(InternalKrosstalkApi::class)
    @KrosstalkPluginApi
    public operator fun get(argument: String): Serializer<Any?, S> = (map[argument]
        ?: throw MissingSerializerException(argument, map.keys, false)) as Serializer<Any?, S>

    /**
     * Serialize all arguments, throwing [MissingSerializerException] if a serializer is missing.
     */
    @KrosstalkPluginApi
    public fun serializeAll(arguments: Map<String, *>): Map<String, S> = arguments.mapValues { serializeArgument(it.key, it.value) }

    /**
     * Deserialize all arguments, throwing [MissingSerializerException] if a serializer is missing.
     */
    @KrosstalkPluginApi
    public fun deserializeAll(arguments: Map<String, S>): Map<String, Any?> = arguments.mapValues { deserializeArgument<Any?>(it.key, it.value) }

    /**
     * Serialize an argument, throwing [MissingSerializerException] if the serializer is missing.
     */
    @KrosstalkPluginApi
    public fun <T> serializeArgument(key: String, value: T): S = this[key].serialize(value)

    /**
     * Deserialize an argument, throwing [MissingSerializerException] if the serializer is missing.
     */
    @KrosstalkPluginApi
    public fun <T> deserializeArgument(key: String, value: S): T = this[key].deserialize(value) as T
}