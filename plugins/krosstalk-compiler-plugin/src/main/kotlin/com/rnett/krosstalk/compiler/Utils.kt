package com.rnett.krosstalk.compiler

import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.addToStdlib.assertedCast

fun Iterable<IrConstructorCall>.getAnnotation(name: FqName): IrConstructorCall? =
    firstOrNull { it.symbol.owner.parentAsClass.fqNameWhenAvailable == name }

fun Iterable<IrConstructorCall>.getAnnotations(name: FqName): List<IrConstructorCall> =
    filter { it.symbol.owner.parentAsClass.fqNameWhenAvailable == name }

fun Iterable<IrConstructorCall>.hasAnnotation(name: FqName) = any { it.symbol.owner.parentAsClass.fqNameWhenAvailable == name }

fun IrType.typeArgument(index: Int) =
    assertedCast<IrSimpleType> { "$this is not a simple type" }.arguments[0].typeOrNull
        ?: error("Type argument $index of $this is not a type (is it a wildcard?)")

fun IrBuilderWithScope.irJsExprBody(expression: IrExpression) = irBlockBody {
    +irReturn(expression)
}