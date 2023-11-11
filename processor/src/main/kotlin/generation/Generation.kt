package com.rnett.krosstalk.processor.generation

import com.google.devtools.ksp.processing.CodeGenerator
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ksp.writeTo
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

object Generation {
    private val generatorFactories: List<(KrosstalkClass) -> Generator> = listOf(
        ::SpecGenerator,
        ::ClientGenerator,
        ::ServerGenerator
    )

    fun generate(
        codeGenerator: CodeGenerator,
        klass: KrosstalkClass,
        methods: Sequence<KrosstalkMethod>
    ) {
        val fileSpec = FileSpec.builder(klass.name.packageName, klass.name.simpleNames.joinToString("_"))

        val generators = generatorFactories.map { it(klass) }

        methods.forEach { method ->
            generators.forEach { it.acceptMethod(method) }
        }

        generators.forEach {
            it.addTo(fileSpec)
        }

        fileSpec.build().writeTo(codeGenerator, klass.dependencies)
    }
}

abstract class Generator(protected val klass: KrosstalkClass) {
    abstract fun acceptMethod(method: KrosstalkMethod)

    abstract fun addTo(file: FileSpec.Builder)
}

fun CodeBlock.Builder.newLine(): CodeBlock.Builder {
    add("\n")
    return this
}

inline fun CodeBlock.Builder.indented(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    indent()
    block()
    unindent()
}