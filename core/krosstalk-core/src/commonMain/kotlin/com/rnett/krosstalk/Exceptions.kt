package com.rnett.krosstalk

import com.rnett.krosstalk.endpoint.Endpoint

abstract class KrosstalkException
@InternalKrosstalkApi constructor(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {

    /**
     * An argument that should have been passed was not.
     */
    @OptIn(InternalKrosstalkApi::class)
    class MissingArgument @InternalKrosstalkApi constructor(val methodName: String, val subMessage: String) : KrosstalkException(
        "Method $methodName: $subMessage")

    /**
     * An argument was passed as the wrong type.
     */
    @OptIn(InternalKrosstalkApi::class)
    class WrongArgumentType @InternalKrosstalkApi constructor(val methodName: String, val subMessage: String) : KrosstalkException(
        "Method $methodName: $subMessage")

    /**
     * An unknown parameter was used in an `@KrosstalkEndpoint` template.
     */
    @OptIn(InternalKrosstalkApi::class)
    class EndpointUnknownArgument @InternalKrosstalkApi constructor(
        val methodName: String,
        val endpointTemplate: Endpoint,
        val missingParam: String,
        val knownParams: Set<String>,
    ) : KrosstalkException(
        "Endpoint template \"$endpointTemplate\" for method $methodName used parameter $missingParam, but it is not a known parameter.  Known parameters: $knownParams."
    )

    /**
     * A Krosstalk call failed.
     */
    @OptIn(InternalKrosstalkApi::class)
    open class CallFailure @InternalKrosstalkApi constructor(
        val methodName: String,
        val httpStatusCode: Int,
        message: String = "Krosstalk method $methodName failed with HTTP status code $httpStatusCode",
    ) : KrosstalkException(message)

    /**
     * An error that was caused by an issue with the compiler plugin.  None of these should happen without a compiler error being thrown if the compiler plugin works properly.
     */
    @OptIn(InternalKrosstalkApi::class)
    open class CompilerError @InternalKrosstalkApi constructor(message: String, cause: Throwable? = null) :
        KrosstalkException(
            "${message.trimEnd()}  This should be impossible without causing a compiler error, please report as a bug.",
            cause
        )

    @Suppress("unused")
    @OptIn(InternalKrosstalkApi::class)
    class CallFromClientSide @PublishedApi internal constructor(methodName: String) :
        CompilerError("Somehow, method $methodName had it's call lambda called on the client side.  This should be impossible, please report as a bug.")

    /**
     * A client side only annotation was used on a method that could have a server annotation.
     */
    @OptIn(InternalKrosstalkApi::class)
    class ClientOnlyAnnotationOnServer @PublishedApi internal constructor(message: String) : CompilerError(message)

    /**
     * A serializer was missing for an argument.
     */
    @OptIn(InternalKrosstalkApi::class)
    class MissingSerializer @InternalKrosstalkApi constructor(val argument: String, val known: Set<String>) :
        CompilerError(
            "Missing serializer for argument $argument.  Known: $known."
        )

}