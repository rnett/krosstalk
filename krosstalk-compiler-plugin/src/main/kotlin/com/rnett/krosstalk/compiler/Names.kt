package com.rnett.krosstalk.compiler

import org.jetbrains.kotlin.name.FqName

object Names {
    val KrosstalkHost = FqName("com.rnett.krosstalk.annotations.KrosstalkHost")
    val KrosstalkMethod = FqName("com.rnett.krosstalk.annotations.KrosstalkMethod")
    val ScopeHolder = FqName("com.rnett.krosstalk.ScopeHolder")
    val addMethod = FqName("com.rnett.krosstalk.Krosstalk.addMethod")
    val MethodTypes = FqName("com.rnett.krosstalk.MethodTypes")

    val typeOf = FqName("kotlin.reflect.typeOf")
    val to = FqName("kotlin.to")
    val mapOf = FqName("kotlin.collections.mapOf")
    val listOf = FqName("kotlin.collections.listOf")
    val Pair = FqName("kotlin.Pair")
    val KType = FqName("kotlin.reflect.KType")
}