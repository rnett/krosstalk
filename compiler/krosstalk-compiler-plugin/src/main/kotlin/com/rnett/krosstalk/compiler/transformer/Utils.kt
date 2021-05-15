package com.rnett.krosstalk.compiler.transformer

import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeOrNull

/**
 * Get all used types in a type declaration, i.e. the outer type and all of the types in any type parameters
 */
fun IrType.allTypes(): List<IrType> {
    if (this !is IrSimpleType) return listOf(this)
    return listOf(this) + arguments.flatMap { it.typeOrNull?.allTypes().orEmpty() }
}
