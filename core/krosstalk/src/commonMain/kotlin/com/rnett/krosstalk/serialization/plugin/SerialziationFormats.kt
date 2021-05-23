package com.rnett.krosstalk.serialization.plugin

import com.rnett.krosstalk.KrosstalkPluginApi

@KrosstalkPluginApi
public interface SerializedFormatTransformer<S> {
    /**
     * Transform [S] to a [ByteArray].
     */
    public fun toByteArray(data: S): ByteArray

    /**
     * Transform a [ByteArray] to [S].
     */
    public fun fromByteArray(data: ByteArray): S

    /**
     * Transform [S] to a [String].
     */
    public fun toString(data: S): String

    /**
     * Transform a [String] to [S].
     */
    public fun fromString(data: String): S
}

@KrosstalkPluginApi
public object StringTransformer : SerializedFormatTransformer<String> {
    override fun toByteArray(data: String): ByteArray = data.encodeToByteArray()

    override fun fromByteArray(data: ByteArray): String = data.decodeToString()

    override fun toString(data: String): String = data

    override fun fromString(data: String): String = data
}

@OptIn(ExperimentalUnsignedTypes::class)
private fun UByte.toHex(): String = toString(16).let {
    if (it.length == 1)
        "0$it"
    else
        it
}.uppercase()

@OptIn(ExperimentalUnsignedTypes::class)
private fun ByteArray.toHexString(): String = toUByteArray().joinToString("") { it.toHex() }

@OptIn(ExperimentalUnsignedTypes::class)
private fun String.hexToBytes(): ByteArray = chunked(2) { it.toString().toInt(16).toUByte() }.toUByteArray().toByteArray()

@KrosstalkPluginApi
public object ByteTransformer : SerializedFormatTransformer<ByteArray> {
    override fun toByteArray(data: ByteArray): ByteArray = data

    override fun fromByteArray(data: ByteArray): ByteArray = data

    override fun toString(data: ByteArray): String = data.toHexString()

    override fun fromString(data: String): ByteArray = data.hexToBytes()
}