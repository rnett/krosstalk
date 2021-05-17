package com.rnett.krosstalk


/**
 * The key used for instance/dispatch receiver parameters/arguments in parameter/argument maps and [KrosstalkEndpoint] templates.
 */
public const val instanceReceiver: String = "\$instanceReceiver"

/**
 * The key used for extension receiver parameters/arguments in parameter/argument maps and [KrosstalkEndpoint] templates.
 */
public const val extensionReceiver: String = "\$extensionReceiver"

/**
 * The key to use the method's name in a [KrosstalkEndpoint] template.
 */
public const val methodName: String = "\$methodName"

/**
 * The key to use the Krosstalk object's [Krosstalk.endpointPrefix] in a [KrosstalkEndpoint] template.
 */
public const val krosstalkPrefix: String = "\$krosstalkPrefix"

/**
 * The default endpoint of krosstalk methods.
 */
public const val defaultEndpoint: String = "$krosstalkPrefix/$methodName"

/**
 * The default HTTP method for krosstalk methods.
 */
public const val defaultEndpointHttpMethod: String = "POST"