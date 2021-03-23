package com.rnett.krosstalk


/**
 * The key used for instance/dispatch receiver parameters/arguments in parameter/argument maps and [KrosstalkEndpoint] templates.
 */
const val instanceParameterKey = "\$instance"

/**
 * The key used for extension receiver parameters/arguments in parameter/argument maps and [KrosstalkEndpoint] templates.
 */
const val extensionParameterKey = "\$extension"

/**
 * The key to use the method's name in a [KrosstalkEndpoint] template.
 */
const val methodNameKey = "\$name"

/**
 * The key to use the Krosstalk object's [Krosstalk.endpointPrefix] in a [KrosstalkEndpoint] template.
 */
const val prefixKey = "\$prefix"

const val defaultEndpoint = "{$prefixKey}/{$methodNameKey}"
const val defaultEndpointMethod = "POST"

