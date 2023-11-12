package com.rnett.krosstalk.processor

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.rnett.krosstalk.processor.generation.Generation
import com.rnett.krosstalk.processor.generation.KrosstalkClass
import com.rnett.krosstalk.processor.generation.KrosstalkMethod
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName

class KrosstalkSymbolProcessor(
    val codeGenerator: CodeGenerator,
    val logger: KSPLogger
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getSymbolsWithAnnotation(References.KrosstalkAnnotation)
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.containingFile != null }
            .forEach {
                it.processClass()
            }

        return emptyList()
    }

    private fun KSClassDeclaration.processClass() {
        if (classKind != ClassKind.INTERFACE) {
            logger.error("Only interfaces can be annotated with @Krosstalk", this)
            return
        }

        if (typeParameters.isNotEmpty()) {
            logger.error("@Krosstalk interfaces may not be generic", this)
            return
        }

        if (companionObject() == null) {
            logger.warn("@Krosstalk classes are recommended to have a companion object", this)
        }

        if (superTypes.count { it.toTypeName() != ANY } != 0)
            logger.warn(
                "@Krosstalk classes having supertypes is discouraged, since they must implement all methods from them",
                this
            )

        Generation.generate(
            codeGenerator,
            toKrosstalkClass(),
            this.getDeclaredFunctions()
                .filter { it.shouldGenerateFor() }
                .map { it.toKrosstalkMethod() }
        )

    }

    private fun KSClassDeclaration.toKrosstalkClass(): KrosstalkClass {
        return KrosstalkClass(
            toClassName(),
            companionObject() != null,
            Dependencies(false, containingFile!!)
        )
    }

    private fun KSFunctionDeclaration.shouldGenerateFor(): Boolean {
        if (!isAbstract)
            return false

        if (typeParameters.isNotEmpty()) {
            logger.error("Krosstalk methods may not have type parameters", this)
            return false
        }

        if (!modifiers.contains(Modifier.SUSPEND)) {
            logger.error("Krosstalk methods must be suspend", this)
            return false
        }

        if (parameters.any {
                if (it.hasDefault) {
                    logger.error("Krosstalk methods may not have parameters with default arguments", it)
                    return@any true
                }
                false
            }) {
            return false
        }

        return true
    }

    private fun KSFunctionDeclaration.toKrosstalkMethod(): KrosstalkMethod {
        return KrosstalkMethod(
            simpleName.getShortName(),
            parameters.associate {
                it.name!!.getShortName() to it.type
            },
            returnType!!
        )
    }
}

fun KSClassDeclaration.companionObject() = this.declarations.find { it is KSClassDeclaration && it.isCompanionObject }