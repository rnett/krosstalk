package com.rnett.krosstalk.serialization


interface SerializedFormatTransformer<S>{
    /**
     * Transform [S] to a [ByteArray].
     */
    fun toByteArray(data: S): ByteArray

    /**
     * Transform a [ByteArray] to [S].
     */
    fun fromByteArray(data: ByteArray): S

    /**
     * Transform [S] to a [String].
     */
    fun toString(data: S): String

    /**
     * Transform a [String] to [S].
     */
    fun fromString(data: String): S
}

object StringTransformer: SerializedFormatTransformer<String>{
    override fun toByteArray(data: String): ByteArray = data.encodeToByteArray()

    override fun fromByteArray(data: ByteArray): String = data.decodeToString()

    override fun toString(data: String): String = data

    override fun fromString(data: String): String = data
}

@OptIn(ExperimentalUnsignedTypes::class)
fun UByte.toHex() = toString(16).let {
    if(it.length == 1)
        "0$it"
    else
        it
}.capitalize()

@OptIn(ExperimentalUnsignedTypes::class)
fun ByteArray.toHexString() = toUByteArray().joinToString("") { it.toHex() }

@OptIn(ExperimentalUnsignedTypes::class)
fun String.hexToBytes() = chunked(2){ it.toString().toInt(16).toUByte() }.toUByteArray().toByteArray()

object ByteTransformer : SerializedFormatTransformer<ByteArray>{
    override fun toByteArray(data: ByteArray): ByteArray = data

    override fun fromByteArray(data: ByteArray): ByteArray = data

    override fun toString(data: ByteArray): String = data.toHexString()

    override fun fromString(data: String): ByteArray = data.hexToBytes()
}