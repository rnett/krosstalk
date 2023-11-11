package com.rnett.krosstalk.metadata

import kotlin.reflect.KType

public data class MethodType(val parameters: Map<String, ParameterType>, val returnType: KType)