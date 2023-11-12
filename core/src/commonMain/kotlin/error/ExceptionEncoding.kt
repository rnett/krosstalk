package com.rnett.krosstalk.error

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
internal object ExceptionEncoding {

    private val base64 = Base64.UrlSafe

    private const val seperator = "||"
    private const val prefix = "[[KROSSTALK_EXCEPTION$seperator"
    private const val suffix = "]]"

    fun encode(type: KrosstalkResultStatus, throwable: Throwable, expose: Boolean): ByteArray {
        val encodedName =
            base64.encode((throwable::class.qualifiedName.takeIf { expose }.orEmpty()).encodeToByteArray())
        val encodedMessage = base64.encode((throwable.message.takeIf { expose }.orEmpty()).encodeToByteArray())
        return buildString {
            append(prefix)
            append(type.name)
            append(seperator)
            append(encodedName)
            append(seperator)
            append(encodedMessage)
        }.encodeToByteArray()
    }

    fun tryDecode(data: ByteArray): KrosstalkException? {
        try {
            val string = data.decodeToString()

            if (!(string.startsWith(prefix) && string.endsWith(suffix)))
                return null

            val parts = string.removePrefix(prefix).removeSuffix(suffix).split(seperator)
            if (parts.size != 3)
                return null

            val type = KrosstalkResultStatus.valueOf(parts[0])
            val exceptionType = base64.decode(parts[1]).decodeToString()
            val exceptionMessage = base64.decode(parts[2]).decodeToString()

            return KrosstalkException(type, exceptionType, exceptionMessage)
        } catch (t: Throwable) {
            return null
        }
    }
}