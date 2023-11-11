package com.rnett.krosstalk.processor.generation

import com.rnett.krosstalk.processor.References
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

abstract class SubclassGenerator(klass: KrosstalkClass, partName: String) : Generator(klass) {
    protected val classBuilder = TypeSpec.classBuilder(klass.partName(partName))

    init {
        classBuilder.apply {
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
                SharedGenerationConstants.specPropertyName,
                References.KrosstalkSpec.parameterizedBy(klass.name)
            ).apply {
                addModifiers(KModifier.OVERRIDE)
                getter(FunSpec.getterBuilder().apply {
                    addCode("%M", klass.specProperty)
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