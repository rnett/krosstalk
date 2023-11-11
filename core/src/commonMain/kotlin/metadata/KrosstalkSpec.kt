package com.rnett.krosstalk.metadata

@Suppress("unused")
public data class KrosstalkSpec<T>(
    val fullyQualifiedName: String,
    val shortName: String,
    val methods: Map<String, MethodType>
)