package com.rnett.krosstalk.error

import com.rnett.krosstalk.error.KrosstalkResultStatus.*

private fun exceptionTypeMessage(exceptionType: String?) = exceptionType ?: "Unknown/redacted type"
private fun exceptionMessageMessage(exceptionMessage: String?) = exceptionMessage ?: "Unknown/redacted message"

public data class KrosstalkException(
    val kind: KrosstalkResultStatus,
    val exceptionClass: String?,
    val exceptionMessage: String?
) : RuntimeException(
    when (kind) {
        SUCCESS -> "The Krosstalk call succeeded, but also encountered an error (somehow): ${
            exceptionTypeMessage(
                exceptionClass
            )
        }: ${exceptionMessageMessage(exceptionMessage)}"

        USER_ERROR -> "The Krosstalk call encountered an error in the server method's implementation: ${
            exceptionTypeMessage(
                exceptionClass
            )
        }: ${exceptionMessageMessage(exceptionMessage)}"

        KROSSTALK_ERROR -> "The Krosstalk call encountered an error in the server's Krosstalk infrastructure: ${
            exceptionTypeMessage(
                exceptionClass
            )
        }: ${exceptionMessageMessage(exceptionMessage)}"
    }
) {

}