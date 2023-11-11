package com.rnett.krosstalk.processor.generation

import com.rnett.krosstalk.processor.References
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toTypeName

class ClientGenerator(klass: KrosstalkClass) : SubclassGenerator(klass, "Client") {

    companion object {
        const val baseUrl = "baseUrl"
        const val requestMaker = "requestMaker"
        const val serialization = "serialization"
        const val argValues = "argValues"
    }

    override fun TypeSpec.Builder.start() {
        superclass(References.KrosstalkClient.parameterizedBy(klass.name))

        primaryConstructor(FunSpec.constructorBuilder().apply {
            addParameter(baseUrl, STRING)
            addParameter(requestMaker, References.RequestMaker)
            addParameter(serialization, References.KrosstalkClientSerialization)
        }.build())

        addSuperclassConstructorParameter("%L", baseUrl)
        addSuperclassConstructorParameter("%L", requestMaker)
        addSuperclassConstructorParameter("%L", serialization)
    }


    override fun acceptMethod(method: KrosstalkMethod) {
        classBuilder.addFunction(
            FunSpec.builder(method.name).apply {
                addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
                method.parmeters.forEach { name, type ->
                    addParameter(name, type.toTypeName())
                }
                returns(method.returnType.toTypeName())

                addCode(CodeBlock.builder().apply {
                    addStatement("val %L = %M(", argValues, References.mapOf).newLine()
                    indented {
                        method.parmeters.keys.forEach {
                            addStatement("%S %M %L", it, References.to, it)
                        }
                    }
                    addStatement(")")
                    addStatement(
                        "return %M(%S, %L) as %T",
                        References.KrosstalkClientInvoke,
                        method.name,
                        argValues,
                        method.returnType.toTypeName()
                    )
                }.build())
            }.build()
        )
    }
}