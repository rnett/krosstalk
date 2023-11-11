package com.rnett.krosstalk.metadata

import com.rnett.krosstalk.server.KrosstalkMethodNotFoundException

@Suppress("unused")
public data class KrosstalkSpec<T>(
    val fullyQualifiedName: String,
    val shortName: String,
    val methods: Map<String, MethodType>
) {
    public fun findMethod(name: String): ResolvedMethod? = methods[name]?.let { ResolvedMethod(this, name, it) }
    public fun method(name: String): ResolvedMethod = findMethod(name) ?: throw KrosstalkMethodNotFoundException(
        fullyQualifiedName,
        name
    )
}

public data class ResolvedMethod(val spec: KrosstalkSpec<*>, val methodName: String, val types: MethodType) {
    val methodFullyQualifiedName: String = spec.fullyQualifiedName + "." + methodName
}