package com.rnett.krosstalk.compiler

import com.rnett.krosstalk.compiler.naming.Package
import com.rnett.krosstalk.compiler.naming.RootPackage
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.isVararg

const val krosstalkPackage = "com.rnett.krosstalk"
const val annotationPackage = "$krosstalkPackage.annotations"

fun firstParamIsVararg(symbol: IrFunctionSymbol) = symbol.owner.valueParameters.firstOrNull()?.isVararg == true

object Krosstalk : RootPackage(krosstalkPackage) {
    object Annotations : Package by -"annotations" {
        val KrosstalkMethod by Class()
        val ClientOnly by Class()
        val MustMatch by Class()
    }

    val KrosstalkResult by Class()
    val ScopeHolder by Class() //TODO this is from https://youtrack.jetbrains.com/issue/KT-44199
    val KrosstalkClient by Class()
    val call by function()
    val clientPlaceholder by function("krosstalkCall")
    val getValueAsOrError by function()

    val MethodTypes by Class(prefix = "serialization")


    val addMethod by function("Krosstalk.addMethod")

}

object Kotlin : RootPackage("kotlin") {
    val typeOf by function("reflect.typeOf")
    val KType by Class("reflect.KType")

    val to by function()
    val Pair by Class()

    val error by function()

    val Annotation by Class()

    object Collections : Package by -"collections" {
        val mapOf by function(filter = ::firstParamIsVararg)
        val setOf by function(filter = ::firstParamIsVararg)
        val listOf by function(filter = ::firstParamIsVararg)

        val Map by Class()
        val Set by Class()
        val List by Class()

        val Iterable by Class()
    }

}