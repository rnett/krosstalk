package com.rnett.krosstalk


/**
 * A method was not registered with it's Krosstalk object.
 */
@OptIn(InternalKrosstalkApi::class)
class MissingMethodException @PublishedApi internal constructor(val krosstalkObject: Krosstalk, val methodName: String) :
    KrosstalkException.CompilerError(
        "Krosstalk $krosstalkObject does not have a registered method named $methodName.  Known methods: ${krosstalkObject.methods}."
    )