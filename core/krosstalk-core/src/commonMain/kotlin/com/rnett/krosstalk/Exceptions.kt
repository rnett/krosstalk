package com.rnett.krosstalk

public abstract class KrosstalkException
@InternalKrosstalkApi constructor(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {

    /**
     * An error that was caused by an issue with the compiler plugin.  None of these should happen without a compiler error being thrown if the compiler plugin works properly.
     */
    @OptIn(InternalKrosstalkApi::class)
    public open class CompilerError @InternalKrosstalkApi constructor(message: String, cause: Throwable? = null) :
        KrosstalkException(
            "${message.trimEnd()}  This should be impossible without causing a compiler error, please report as a bug.",
            cause
        )

}