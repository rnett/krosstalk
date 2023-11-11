package com.rnett.krosstalk.processor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.MemberName.Companion.member

object References {
    const val KrosstalkAnnotation = "com.rnett.krosstalk.Krosstalk"

    val KrosstalkSpec = ClassName.bestGuess("com.rnett.krosstalk.metadata.KrosstalkSpec")
    val MethodType = ClassName.bestGuess("com.rnett.krosstalk.metadata.MethodType")
    val ParameterType = ClassName.bestGuess("com.rnett.krosstalk.metadata.ParameterType")

    val KrosstalkClient = ClassName.bestGuess("com.rnett.krosstalk.client.KrosstalkClient")
    val RequestMaker = ClassName.bestGuess("com.rnett.krosstalk.client.RequestMaker")
    val KrosstalkClientSerialization = ClassName.bestGuess("com.rnett.krosstalk.client.KrosstalkClientSerialization")
    val KrosstalkClientInvoke = KrosstalkClient.member("invoke")

    val KrosstalkServer = ClassName.bestGuess("com.rnett.krosstalk.server.KrosstalkServer")
    val KrosstalkServerSerialization = ClassName.bestGuess("com.rnett.krosstalk.server.KrosstalkServerSerialization")

    val KrosstalkMethodNotFoundException =
        ClassName.bestGuess("com.rnett.krosstalk.server.KrosstalkMethodNotFoundException")

    val mapOf = MemberName("kotlin.collections", "mapOf")
    val to = MemberName("kotlin", "to")
    val typeOf = MemberName("kotlin.reflect", "typeOf")
}