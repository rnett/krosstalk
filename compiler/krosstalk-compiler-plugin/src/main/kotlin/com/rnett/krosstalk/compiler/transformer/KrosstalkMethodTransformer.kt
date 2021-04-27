package com.rnett.krosstalk.compiler.transformer

import com.rnett.krosstalk.InternalKrosstalkApi
import com.rnett.krosstalk.KrosstalkPluginApi
import com.rnett.krosstalk.compiler.KotlinAddons
import com.rnett.krosstalk.compiler.Krosstalk
import com.rnett.plugin.ir.IrTransformer
import com.rnett.plugin.ir.withValueArguments
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isSubtypeOf
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.multiplatform.findExpects
import kotlin.math.absoluteValue

@OptIn(ExperimentalStdlibApi::class, InternalKrosstalkApi::class, KrosstalkPluginApi::class)
class KrosstalkMethodTransformer(
    context: IrPluginContext,
    messageCollector: MessageCollector,
) : IrTransformer(context, messageCollector) {

    val stringType = context.irBuiltIns.stringType

    val Headers = context.referenceTypeAlias(FqName("com.rnett.krosstalk.Headers"))!!.owner.expandedType

    val addedInitalizers = mutableMapOf<FqName, IrAnonymousInitializer>()
    val seenNames = mutableMapOf<FqName, MutableSet<String>>()

    fun IrFunction.paramHash() = this.symbol.signature!!.hashCode().absoluteValue.toString(36)

    val tripleTypes by lazy {
        listOf(
            KotlinAddons.Reflect.KClass.resolveTypeWith(),
            context.irBuiltIns.intType,
            context.irBuiltIns.stringType
        )
    }

    fun IrBuilderWithScope.getValueOrError(
        methodName: String,
        map: IrExpression,
        type: IrType,
        key: String,
        default: IrBody?,
        nullError: String,
        typeError: String,
        keyType: IrType = stringType,
    ) =
        irCall(Krosstalk.getValueAsOrError(), type).apply {
            putTypeArgument(0, keyType)
            putTypeArgument(1, type)

            extensionReceiver = map

            withValueArguments(
                methodName.asConst(),
                key.asConst(),
                (default != null).asConst(),
                nullError.asConst(),
                typeError.asConst(),
                default?.let { lambdaArgument(buildLambda(type) { body = it }) }
            )
        }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    fun IrClass.expect(): IrClass? {
        if (!this.descriptor.isActual)
            return null
        else
            return context.symbolTable.referenceClass(
                this.descriptor.findExpects().single() as ClassDescriptor
            ).owner
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {

        if (declaration.isExpect) {
            if (declaration.annotations.hasAnnotation(Krosstalk.Annotations.KrosstalkMethod.fqName))
                KrosstalkFunction(declaration, this).check()
            return super.visitSimpleFunction(declaration)
        }

        var isKrosstalk: Boolean = declaration.annotations.hasAnnotation(Krosstalk.Annotations.KrosstalkMethod.fqName)

        if (declaration.descriptor.isActual) {
            val expect = context.symbolTable.referenceFunction(
                declaration.descriptor.findExpects().single() as CallableDescriptor
            ).owner


            if (expect.hasAnnotation(Krosstalk.Annotations.KrosstalkMethod())) {
                isKrosstalk = true
            }
        }

        if (isKrosstalk) {
            KrosstalkFunction(declaration, this).apply {
                check()
                transform()
            }
        }
        return super.visitSimpleFunction(declaration)
    }

    override fun visitClassNew(declaration: IrClass): IrStatement {
        if (declaration.defaultType.isSubtypeOf(
                Krosstalk.Krosstalk.resolveTypeWith(),
                context.irBuiltIns
            ) && !declaration.isExpect
        ) {
            if (declaration.isObject && !declaration.isCompanion && !declaration.isAnonymousObject) {
                KrosstalkClass(declaration, this).apply {
                    check()
                    registerScopes()
                }
            } else {
                messageCollector.reportError("Can't have class extending Krosstalk that isn't an object.", declaration)
            }
        }
        return super.visitClassNew(declaration)
    }
}