package com.rnett.krosstalk.processor.generation

import com.rnett.krosstalk.processor.References
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile

abstract class SubclassGenerator(klass: KrosstalkClass, partName: String) : Generator(klass) {
    protected val generatedClassName = klass.partName(partName)
    protected val classBuilder = TypeSpec.classBuilder(generatedClassName)

    init {
        classBuilder.apply {
            klass.dependencies.originatingFiles.forEach {
                addOriginatingKSFile(it)
            }
            setupClassWrapper()
            addSpecProperty()
        }

    }

    private fun TypeSpec.Builder.setupClassWrapper() {
        addSuperinterface(klass.name)
        start()
    }

    protected abstract fun TypeSpec.Builder.start()

    private fun TypeSpec.Builder.addSpecProperty() {
        addProperty(
            PropertySpec.builder(
                klass.specProperty.simpleName,
                References.KrosstalkSpec.parameterizedBy(klass.name)
            ).apply {
                addModifiers(KModifier.OVERRIDE)
                getter(FunSpec.getterBuilder().apply {
                    addCode("return %L", SharedGenerationConstants.fileSpecPropertyName)
                }.build())
            }.build()
        )
    }

    open fun TypeSpec.Builder.finish() {

    }

    override fun addTo(file: FileSpec.Builder) {
        classBuilder.finish()
        file.addType(classBuilder.build())
    }
}