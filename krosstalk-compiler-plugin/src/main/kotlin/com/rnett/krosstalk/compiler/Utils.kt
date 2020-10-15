package com.rnett.krosstalk.compiler

import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.name.FqName


fun Iterable<IrConstructorCall>.getAnnotation(name: FqName): IrConstructorCall? = firstOrNull { it.symbol.owner.parentAsClass.fqNameWhenAvailable == name }
fun Iterable<IrConstructorCall>.getAnnotations(name: FqName): List<IrConstructorCall> = filter { it.symbol.owner.parentAsClass.fqNameWhenAvailable == name }
fun Iterable<IrConstructorCall>.hasAnnotation(name: FqName) = any { it.symbol.owner.parentAsClass.fqNameWhenAvailable == name }
