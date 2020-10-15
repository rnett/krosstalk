package com.rnett.krosstalk.compiler

import com.rnett.krosstalk.compiler.naming.Reference
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.allParameters
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.IrFunctionBuilder
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrAnonymousInitializerImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.IrBindableSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import kotlin.reflect.KProperty

class GetterDelegate<T>(val get: () -> T) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = get()
}

fun <T> getter(block: () -> T) = GetterDelegate(block)

interface HasContext {
    val context: IrPluginContext


    fun <R : IrBindableSymbol<*, *>> Reference<R>.get(): R = get(context)
    operator fun <R : IrBindableSymbol<*, *>> Reference<R>.unaryPlus() = get()
    operator fun <R : IrBindableSymbol<*, *>> Reference<R>.invoke() = get()

    fun FqName.klass() = getter { context.referenceClass(this) ?: error("$this not found") }
    fun FqName.topLevelFunction() = getter {
        context.referenceFunctions(this).singleOrNull() ?: error("$this not found")
    }

    fun FqName.klassFunction(function: String) = getter {
        (context.referenceClass(this) ?: error("Class $this not found")).getSimpleFunction(function)
            ?: error("Function $function not found in $this")
    }

    fun IrClass.addAnonymousInitializer(builder: IrAnonymousInitializer.() -> Unit): IrAnonymousInitializer {
        return IrAnonymousInitializerImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            IrDeclarationOrigin.DEFINED,
            IrAnonymousInitializerSymbolImpl(this.descriptor)
        ).apply(builder).also {
            this.addMember(it)
            it.parent = this
        }
    }

    fun String.asConst() = IrConstImpl.string(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.stringType, this)
    fun Int.asConst() = IrConstImpl.int(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.intType, this)
    fun Boolean.asConst() = IrConstImpl.boolean(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.booleanType, this)

    fun nullConst(type: IrType) = IrConstImpl.constNull(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type)

    fun buildLambda(
        returnType: IrType,
//            parent: IrDeclarationParent,
        funBuilder: IrFunctionBuilder.() -> Unit = {},
        funApply: IrSimpleFunction.() -> Unit
    ): IrSimpleFunction = buildFun {
        name = Name.special("<anonymous>")
        this.returnType = returnType
        this.origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
        this.visibility = Visibilities.LOCAL
        funBuilder()
    }.apply {
//        this.parent = parent
        funApply()
    }

    fun lambdaArgument(
            lambda: IrSimpleFunction,
            type: IrType = run {
                val base = if (lambda.isSuspend)
                    context.irBuiltIns.suspendFunction(lambda.allParameters.size)
                else
                    context.irBuiltIns.function(lambda.allParameters.size)

                base.typeWith(lambda.allParameters.map { it.type } + lambda.returnType)
            }
    ) = IrFunctionExpressionImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            type,
            lambda,
            IrStatementOrigin.LAMBDA
    )

    fun varargOf(elementType: IrType, elements: Iterable<IrExpression>) = IrVarargImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        context.irBuiltIns.arrayClass.typeWith(elementType),
        elementType,
        elements.toList()
    )

    fun IrSymbolOwner.newBuilder() = DeclarationIrBuilder(context, symbol, startOffset, endOffset)

    fun IrSymbolOwner.newBuilder(block: DeclarationIrBuilder.() -> Unit) = DeclarationIrBuilder(context, symbol, startOffset, endOffset).apply(block)

    fun IrSymbolOwner.newSingleStatementBuilder() = IrSingleStatementBuilder(context, Scope(symbol), startOffset, endOffset)

    fun <T : IrElement> IrSymbolOwner.buildStatement(block: IrSingleStatementBuilder.() -> T) = IrSingleStatementBuilder(context, Scope(symbol), startOffset, endOffset).build(block)

    fun IrBuilderWithScope.irJsExperBody(expression: IrExpression) = irBlockBody { +irReturn(expression) }

    fun IrBuilderWithScope.irJsExperBody(exprBody: IrExpressionBody) = irBlockBody { +irReturn(exprBody.expression) }
}