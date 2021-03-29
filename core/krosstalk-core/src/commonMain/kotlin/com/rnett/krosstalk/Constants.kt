package com.rnett.krosstalk


/**
 * The key used for instance/dispatch receiver parameters/arguments in parameter/argument maps and [KrosstalkEndpoint] templates.
 */
const val instanceReceiver = "\$instanceReceiver"

/**
 * The key used for extension receiver parameters/arguments in parameter/argument maps and [KrosstalkEndpoint] templates.
 */
const val extensionReceiver = "\$extensionReceiver"

/**
 * The key to use the method's name in a [KrosstalkEndpoint] template.
 */
const val methodName = "\$methodName"

/**
 * The key to use the Krosstalk object's [Krosstalk.endpointPrefix] in a [KrosstalkEndpoint] template.
 */
const val krosstalkPrefix = "\$krosstalkPrefix"

const val defaultEndpoint = "$krosstalkPrefix/$methodName"
const val defaultEndpointMethod = "POST"

const val exception = "\$exception"
const val exceptionMessage = "\$exceptionMessage"
const val exceptionStacktrace = "\$exceptionStacktrace"