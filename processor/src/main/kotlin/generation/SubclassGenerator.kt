package com.rnett.krosstalk.processor.generation

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
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
        }

    }

    private fun TypeSpec.Builder.setupClassWrapper() {
        addSuperinterface(klass.name)
        start()
    }

    protected abstract fun TypeSpec.Builder.start()

    open fun TypeSpec.Builder.finish() {

    }

    override fun addTo(file: FileSpec.Builder) {
        classBuilder.finish()
        file.addType(classBuilder.build())
    }
}