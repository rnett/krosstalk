package com.rnett.krosstalk


/**
 * The key used for instance/dispatch receiver parameters/arguments in parameter/argument maps and [KrosstalkEndpoint] templates.
 */
const val instanceParameter = "\$instance"

/**
 * The key used for extension receiver parameters/arguments in parameter/argument maps and [KrosstalkEndpoint] templates.
 */
const val extensionParameter = "\$extension"

/**
 * The key to use the method's name in a [KrosstalkEndpoint] template.
 */
const val methodName = "\$name"

/**
 * The key to use the Krosstalk object's [Krosstalk.endpointPrefix] in a [KrosstalkEndpoint] template.
 */
const val prefix = "\$prefix"

const val defaultEndpoint = "$prefix/$methodName"
const val defaultEndpointMethod = "POST"

