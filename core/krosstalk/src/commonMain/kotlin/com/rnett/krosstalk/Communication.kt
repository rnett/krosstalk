package com.rnett.krosstalk


@Suppress("unused")
@OptIn(InternalKrosstalkApi::class)
class CallFromClientSideException @PublishedApi internal constructor(methodName: String) :
    KrosstalkException.CompilerError("Somehow, method $methodName had it's call lambda called on the client side.  This should be impossible, please report as a bug.")


const val baseUrlLegalRegex = "-a-zA-Z0-9._*~'()!"

@OptIn(ExperimentalUnsignedTypes::class)
fun String.urlEncode(legal: String = baseUrlLegalRegex): String {
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
fun String.urlDecode(): String {
    return Regex("%([0-9A-F]{2})").replace(this) {
        it.groupValues[1].toUByte(16).toByte().toChar().toString()
    }
}