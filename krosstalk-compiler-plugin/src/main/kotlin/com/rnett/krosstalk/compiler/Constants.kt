package com.rnett.krosstalk.compiler

const val instanceParameterKey = "\$instance"
const val extensionParameterKey = "\$extension"
const val methodNameKey = "\$name"
const val prefixKey = "\$prefix"

const val defaultEndpoint = "{$prefixKey}/{$methodNameKey}"
const val defaultEndpointMethod = "POST"

val keyRegex = Regex("\\{([^}]+?)\\}")