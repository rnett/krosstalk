package com.rnett.krosstalk.processor.generation

import com.google.devtools.ksp.processing.Dependencies
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName

data class KrosstalkClass(
    val name: ClassName,
    val hasCompanionObject: Boolean,
    val dependencies: Dependencies
) {
    val specProperty = MemberName(name.packageName, "spec")

    fun partName(part: String): ClassName =
        ClassName(
            name.packageName,
            name.simpleNames.dropLast(1) + (name.simpleNames.last() + part.replaceFirstChar(Char::titlecase))
        )
}