package com.rnett.krosstalk.compiler

import org.jetbrains.kotlin.name.FqName

object Names {
    //    val KrosstalkHost = FqName("com.rnett.krosstalk.annotations.KrosstalkHost")
    val KrosstalkMethod = FqName("com.rnett.krosstalk.annotations.KrosstalkMethod")
    val ScopeHolder = FqName("com.rnett.krosstalk.ScopeHolder")
    val addMethod = FqName("com.rnett.krosstalk.Krosstalk.addMethod")
    val MethodTypes = FqName("com.rnett.krosstalk.MethodTypes")
    val KrosstalkClient = FqName("com.rnett.krosstalk.KrosstalkClient")
    val call = FqName("com.rnett.krosstalk.call")
    val OptionalNone = FqName("com.rnett.krosstalk.Optional.None")
    val OptionalSome = FqName("com.rnett.krosstalk.Optional.Some")
    val clientPlaceholder = FqName("com.rnett.krosstalk.krosstaklCall")

    val getValueAsOrError = FqName("com.rnett.krosstalk.getValueAsOrError")

    val typeOf = FqName("kotlin.reflect.typeOf")
    val to = FqName("kotlin.to")
    val mapOf = FqName("kotlin.collections.mapOf")
    val Pair = FqName("kotlin.Pair")
    val Map = FqName("kotlin.collections.Map")
    val Iterable = FqName("kotlin.collections.Iterable")
    val KType = FqName("kotlin.reflect.KType")
    val error = FqName("kotlin.error")
}