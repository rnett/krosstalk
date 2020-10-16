package com.rnett.krosstalk

import com.rnett.krosstalk.annotations.KrosstalkEndpoint

sealed class KrosstalkException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {

    /**
     * Some required scopes are missing from a Krosstalk call.
     */
    class MissingScope(val methodName: String, val missing: Set<String>, val activeScopes: Set<String>) : KrosstalkException(
            "Missing required scopes $missing for $methodName.  Active scopes: $activeScopes"
    )

    /**
     * An argument that should have been passed was not.
     */
    class MissingArgument(val methodName: String, val subMessage: String) : KrosstalkException(
            "Method $methodName: $subMessage")

    /**
     * An argument was passed as the wrong type.
     */
    class WrongArgumentType(val methodName: String, val subMessage: String) : KrosstalkException(
            "Method $methodName: $subMessage")

    /**
     * An unknown parameter was used in an [KrosstalkEndpoint] template.
     */
    class EndpointUnknownArgument(
        val methodName: String,
        val endpointTemplate: Endpoint,
        val missingParam: String,
        val knownParams: Set<String>
    ) : KrosstalkException(
        "Endpoint template \"$endpointTemplate\" for method $methodName used parameter $missingParam, but it is not a known parameter.  Known parameters: $knownParams."
    )

    /**
     * A Krosstalk call failed.
     */
    open class CallFailure(
        val methodName: String,
        val httpStatusCode: Int,
        message: String = "Krosstalk method $methodName failed with HTTP status code $httpStatusCode"
    ) : KrosstalkException(message)

    /**
     * An error that was caused by an issue with the compiler plugin.  None of these should happen without a compiler error being thrown if the compiler plugin works properly.
     */
    open class CompilerError @PublishedApi internal constructor(message: String, cause: Throwable? = null) :
        KrosstalkException(
            "${message.trimEnd()}  This should be impossible without causing a compiler error, please report as a bug.",
            cause
        )

    /**
     * A method was not registered with it's Krosstalk object.
     */
    class MissingMethod @PublishedApi internal constructor(val krosstalkObject: Krosstalk, val methodName: String) :
        CompilerError(
            "Krosstalk $krosstalkObject does not have a registered method named $methodName.  Known methods: ${krosstalkObject.methods}."
        )

    /**
     * A client side only annotation was used on a method that could have a server annotation.
     */
    class ClientOnlyAnnotationOnServer @PublishedApi internal constructor(message: String) : CompilerError(message)

    /**
     * A serializer was missing for an argument.
     */
    class MissingSerializer @PublishedApi internal constructor(val argument: String, val known: Set<String>) :
        CompilerError(
            "Missing serializer for argument $argument.  Known: $known."
        )

}