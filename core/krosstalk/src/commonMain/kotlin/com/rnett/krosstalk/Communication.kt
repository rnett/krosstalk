package com.rnett.krosstalk

@InternalKrosstalkApi
public const val KROSSTALK_THROW_EXCEPTION_HEADER_NAME: String = "Krosstalk-Throw-Exception"

@InternalKrosstalkApi
public const val KROSSTALK_UNCAUGHT_EXCEPTION_HEADER_NAME: String = "Krosstalk-Uncaught-Exception"

//TODO make own class so I can use proper merge w/ plus, etc.  Case-insensitivity, too
public typealias Headers = Map<String, List<String>>
public typealias MutableHeaders = MutableMap<String, List<String>>

@OptIn(ExperimentalStdlibApi::class)
@InternalKrosstalkApi
public infix fun MutableHeaders.addHeadersFrom(other: Headers) {
    other.forEach { (k, v) ->
        this[k] = this[k].orEmpty() + v
    }
}

@InternalKrosstalkApi
public fun Headers.withHeader(key: String, value: String): Headers = this.plus(key to this.getOrElse(key) { emptyList() } + value)

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
        "%" + it.value[0].toByte().toUByte().toString(16).toUpperCase().let {
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
        it.groupValues[1].toUByte(16).toByte().toChar().toString()
    }
}