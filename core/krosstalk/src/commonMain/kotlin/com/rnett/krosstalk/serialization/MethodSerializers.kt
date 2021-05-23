package com.rnett.krosstalk.serialization

import com.rnett.krosstalk.InternalKrosstalkApi
import com.rnett.krosstalk.KrosstalkException
import com.rnett.krosstalk.KrosstalkPluginApi
import com.rnett.krosstalk.ServerDefault
import com.rnett.krosstalk.WithHeaders
import com.rnett.krosstalk.extensionReceiver
import com.rnett.krosstalk.instanceReceiver
import com.rnett.krosstalk.result.KrosstalkResult
import com.rnett.krosstalk.serialization.plugin.ArgumentSerializers
import com.rnett.krosstalk.serialization.plugin.SerializationHandler
import com.rnett.krosstalk.serialization.plugin.SerializedFormatTransformer
import com.rnett.krosstalk.serialization.plugin.Serializer
import com.rnett.krosstalk.urlDecode
import com.rnett.krosstalk.urlEncode
import kotlin.reflect.KType

//TODO use value classes

/**
 * A serializer was missing for an argument.
 */
@OptIn(InternalKrosstalkApi::class)
public class MissingSerializerException @InternalKrosstalkApi constructor(
    public val argument: String,
    public val known: Set<String>,
    public val url: Boolean,
) :
    KrosstalkException(
        "Missing ${if (url) "url" else "body"} serializer for argument $argument.  Known: $known."
    )

/**
 * All necessary types for a method.
 * Instance and extension receiver serializers are in the [paramTypes] map with keys [instanceReceiver] and [extensionReceiver], respectively, but can be accessed through their accessors.
 *
 * [KrosstalkResult] and [WithHeaders] return types and [ServerDefault] parameters will be stored as their type params.
 */
@InternalKrosstalkApi
public data class MethodTypes(
    val paramTypes: Map<String, KType>,
    val resultType: KType?,
) {
    val instanceReceiverType: KType? by lazy { paramTypes[instanceReceiver] }
    val extensionReceiverSerializer: KType? by lazy { paramTypes[extensionReceiver] }
}


@InternalKrosstalkApi
public data class MethodSerialization internal constructor(
    private val bodySerializers: MethodArgumentSerializers<*>,
    private val urlArgSerializers: Map<String, TransformedSerializer<*, Any?>>,
    private val returnValueSerializer: TransformedSerializer<*, Any?>?,
) {
    @InternalKrosstalkApi
    public fun serializeReturnValue(value: Any?): ByteArray =
        (returnValueSerializer ?: error("No return value serializer, is the method returning an object?"))
            .serializeToBytes(value)

    @InternalKrosstalkApi
    public fun deserializeReturnValue(value: ByteArray): Any? =
        (returnValueSerializer ?: error("No return value serializer, is the method returning an object?"))
            .deserializeFromBytes(value)

    @InternalKrosstalkApi
    public fun serializeUrlArg(arg: String, value: Any?): String =
        urlArgSerializers.getOrElse(arg) { throw MissingSerializerException(arg, urlArgSerializers.keys, true) }.serializeToString(value)
            .urlEncode()

    @InternalKrosstalkApi
    public fun deserializeUrlArg(arg: String, value: String): Any? =
        urlArgSerializers.getOrElse(arg) { throw MissingSerializerException(arg, urlArgSerializers.keys, true) }
            .deserializeFromString(value.urlDecode())

    @InternalKrosstalkApi
    public fun serializeBodyArguments(arguments: Map<String, *>): ByteArray = bodySerializers.serializeArgumentsToBytes(arguments)

    @InternalKrosstalkApi
    public fun deserializeBodyArguments(arguments: ByteArray): Map<String, *> = bodySerializers.deserializeArgumentsFromBytes(arguments)
}

@InternalKrosstalkApi
@OptIn(KrosstalkPluginApi::class)
internal data class TransformedSerializer<S, T>(val transformer: SerializedFormatTransformer<S>, val serializer: Serializer<T, S>) {
    fun serializeToBytes(data: T): ByteArray = transformer.toByteArray(serializer.serialize(data))
    fun serializeToString(data: T): String = transformer.toString(serializer.serialize(data))

    fun deserializeFromBytes(data: ByteArray): T = serializer.deserialize(transformer.fromByteArray(data))
    fun deserializeFromString(data: String): T = serializer.deserialize(transformer.fromString(data))
}

@InternalKrosstalkApi
@OptIn(KrosstalkPluginApi::class)
internal data class MethodArgumentSerializers<S>(val serialization: SerializationHandler<S>, val argumentSerializers: ArgumentSerializers<S>) {
    fun serializeArgumentsToBytes(arguments: Map<String, *>): ByteArray =
        serialization.transformer.toByteArray(serialization.serializeArguments(arguments, argumentSerializers))

    fun serializeArgumentsToString(arguments: Map<String, *>): String =
        serialization.transformer.toString(serialization.serializeArguments(arguments, argumentSerializers))

    fun deserializeArgumentsFromBytes(arguments: ByteArray): Map<String, *> =
        serialization.deserializeArguments(serialization.transformer.fromByteArray(arguments), argumentSerializers)

    fun deserializeArgumentsFromString(arguments: String): Map<String, *> =
        serialization.deserializeArguments(serialization.transformer.fromString(arguments), argumentSerializers)

    fun <T> serializeArgumentToBytes(key: String, value: T): ByteArray =
        serialization.transformer.toByteArray(serialization.serializeArgument(key, value, argumentSerializers))

    fun <T> serializeArgumentToString(key: String, value: T): String =
        serialization.transformer.toString(serialization.serializeArgument(key, value, argumentSerializers))

    fun <T> deserializeArgumentFromBytes(key: String, value: ByteArray): T =
        serialization.deserializeArgument(key, serialization.transformer.fromByteArray(value), argumentSerializers)

    fun <T> deserializeArgumentFromString(key: String, value: String): T =
        serialization.deserializeArgument(key, serialization.transformer.fromString(value), argumentSerializers)
}