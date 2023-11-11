package com.rnett.krosstalk.processor.generation

import com.rnett.krosstalk.processor.References
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toTypeName

class SpecGenerator(klass: KrosstalkClass) : Generator(klass) {
    private val property =
        PropertySpec.builder(
            SharedGenerationConstants.specPropertyName,
            References.KrosstalkSpec.parameterizedBy(klass.name),
            KModifier.PRIVATE
        )
    private val initializer = CodeBlock.builder()
    private var companionExtension: PropertySpec? = null

    init {
        initializer.apply {
            addStatement("%T<%T>(", References.KrosstalkSpec, klass.name)
            indent()
            addStatement("%S,", klass.name.canonicalName)
            addStatement("%S,", klass.name.simpleName)
            addStatement("%M(", References.mapOf)
            indent()
        }

        if (klass.hasCompanionObject)
            addToInterfaceCompanion()

    }

    override fun acceptMethod(method: KrosstalkMethod) {
        initializer.apply {
            addStatement("%S %M %T(", method.name, References.to, References.MethodType)
            indented {
                addStatement("%M(", References.mapOf)
                indented {
                    method.parmeters.forEach {
                        addStatement(
                            "%S %M %T(%M<%T>()),",
                            it.key,
                            References.to,
                            References.ParameterType,
                            References.typeOf,
                            it.value.toTypeName()
                        )
                    }
                }
                addStatement("),")
                addStatement("%M<%T>()", References.typeOf, method.returnType.toTypeName())
            }
            addStatement(")")
        }
    }

    private fun finish(): PropertySpec {
        initializer.apply {
            unindent().addStatement(")")
            unindent().addStatement(")")
        }

        property.initializer(initializer.build())
        return property.build()
    }

    private fun addToInterfaceCompanion() {
        companionExtension = PropertySpec.builder("SPEC", References.KrosstalkSpec.parameterizedBy(klass.name))
            .apply {
                this.receiver(klass.name.nestedClass("Companion"))
                getter(
                    FunSpec.getterBuilder()
                        .addCode("return %M", klass.specProperty)
                        .build()
                )
            }.build()
    }

    override fun addTo(file: FileSpec.Builder) {
        file.addProperty(finish())
        companionExtension?.let {
            file.addProperty(it)
        }
    }

}