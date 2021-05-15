package com.rnett.krosstalk

@InternalKrosstalkApi
public const val KROSSTALK_THROW_EXCEPTION_HEADER_NAME: String = "Krosstalk-Throw-Exception"

@InternalKrosstalkApi
public const val KROSSTALK_UNCAUGHT_EXCEPTION_HEADER_NAME: String = "Krosstalk-Uncaught-Exception"

@Suppress("unused")
@OptIn(InternalKrosstalkApi::class)
public class CallFromClientSideException @PublishedApi internal constructor(methodName: String) :
    KrosstalkException.CompilerError("Somehow, method $methodName had it's call lambda called on the client side.  This should be impossible, please report as a bug.")


private const val baseUrlLegalRegex = "-a-zA-Z0-9._*~'()!"

@OptIn(ExperimentalUnsignedTypes::class)
@InternalKrosstalkApi
public fun String.urlEncode(legal: String = baseUrlLegalRegex): String {
    val regex = Regex("[^$legal]")
    return this.replace(regex) {
        "%" + it.value[0].code.toUByte().toString(16).uppercase().let {
            if (it.length == 1)
                "0$it"
            else
                it
        }
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
@InternalKrosstalkApi
public fun String.urlDecode(): String {
    return Regex("%([0-9A-F]{2})").replace(this) {
        it.groupValues[1].toUByte(16).toInt().toChar().toString()
    }
}