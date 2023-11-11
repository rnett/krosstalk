package com.rnett.krosstalk.processor.generation

import com.rnett.krosstalk.processor.References
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toTypeName

class ServerGenerator(klass: KrosstalkClass) : SubclassGenerator(klass, "Server") {

    companion object {
        const val serialization = "serialization"
        const val methodName = "methodName"
        const val arguments = "arguments"
        val ANYQ = ANY.copy(nullable = true)
    }

    private val invokeFunction = FunSpec.builder("invoke").apply {
        addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)

        returns(ANYQ)

        addParameter(methodName, STRING)
        addParameter(arguments, MAP.parameterizedBy(STRING, ANYQ))
    }

    override fun TypeSpec.Builder.start() {
        superclass(References.KrosstalkServer.parameterizedBy(klass.name))
        addModifiers(KModifier.ABSTRACT)

        primaryConstructor(FunSpec.constructorBuilder().apply {
            addParameter(serialization, References.KrosstalkServerSerialization)
        }.build())

        addSuperclassConstructorParameter("%L", serialization)
        addSuperclassConstructorParameter("%L", SharedGenerationConstants.fileSpecPropertyName)
    }

    override fun acceptMethod(method: KrosstalkMethod) {
        invokeFunction.addCode(CodeBlock.builder()
            .apply {
                beginControlFlow("if (%L == %S)", methodName, method.name)
                addStatement("return %L(", method.name)
                indented {
                    method.parmeters.forEach { name, type ->
                        addStatement("%L[%S] as %T,", arguments, name, type.toTypeName())
                    }
                }
                addStatement(")")
                endControlFlow()
            }
            .build())
    }

    override fun TypeSpec.Builder.finish() {

        invokeFunction.addCode(
            "\nthrow %T(%S, %S)",
            References.KrosstalkMethodNotFoundException,
            klass.name.canonicalName,
            methodName
        )

        addFunction(invokeFunction.build())
    }

}