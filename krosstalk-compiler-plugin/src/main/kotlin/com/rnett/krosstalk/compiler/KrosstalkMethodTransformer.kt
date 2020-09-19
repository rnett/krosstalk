package com.rnett.krosstalk.compiler

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrAnonymousInitializerImpl
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.isSubtypeOfClass
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.multiplatform.findExpects
import java.io.File

const val instanceParameterKey = "\$instance"
const val extensionParameterKey = "\$extension"

class KrosstalkMethodTransformer(val context: IrPluginContext, val messageCollector: MessageCollector) :
    IrElementTransformerVoidWithContext(), FileLoweringPass {

    private lateinit var file: IrFile
    private lateinit var fileSource: String

    fun IrElement.location(): CompilerMessageLocation? {
        val beforeText = fileSource.substring(0, this.startOffset)
        val line = beforeText.count { it == '\n' } + 1
        val beforeLine = beforeText.substringBeforeLast('\n').length
        val offsetOnLine = this.startOffset - beforeLine
        return CompilerMessageLocation.create(file.path, line, offsetOnLine, null)
    }

    override fun lower(irFile: IrFile) {
        file = irFile
        fileSource = File(irFile.path).readText()

        irFile.transformChildrenVoid()
    }

    fun FqName.klass() = lazy { context.referenceClass(this) ?: error("$this not found") }
    fun FqName.function() = lazy { context.referenceFunctions(this).singleOrNull() ?: error("$this not found") }

    val ScopeHolder by Names.ScopeHolder.klass()
    val addMethod by Names.addMethod.function()
    val typeOf by Names.typeOf.function()
    val mapOf by Names.mapOf.function()
    val listOf by Names.listOf.function()
    val to by Names.to.function()
    val Pair by Names.Pair.klass()
    val MethodTypes by Names.MethodTypes.klass()
    val KType by Names.KType.klass()

    fun IrClass.scopeProps() = properties.filter {
        context.typeTranslator.translateType(it.descriptor.type).isSubtypeOfClass(ScopeHolder)
    }.map { it.name.identifier }.toSet()

    inline fun IrClass.addAnonymousInitializer(builder: IrAnonymousInitializer.() -> Unit): IrAnonymousInitializer {
        return IrAnonymousInitializerImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            IrDeclarationOrigin.DEFINED,
            IrAnonymousInitializerSymbolImpl(this.descriptor)
        ).apply(builder).also {
            this.addMember(it)
        }
    }

    fun String.asConst() = IrConstImpl.string(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.stringType, this)

    fun nullConst(type: IrType) = IrConstImpl.constNull(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type)

    val addedInitalizers = mutableMapOf<IrClassSymbol, IrAnonymousInitializer>()

    fun IrBuilderWithScope.mapOf(keyType: IrType, valueType: IrType, map: Map<IrExpression, IrExpression>) =
        irCall(mapOf).apply {
            putTypeArgument(0, keyType)
            putTypeArgument(1, valueType)
            putValueArgument(0,
                varargOf(
                    Pair.typeWith(keyType, valueType),
                    map.map { (key, value) ->
                        irCall(to).apply {
                            putTypeArgument(0, keyType)
                            putTypeArgument(1, valueType)
                            putValueArgument(0, key)
                            putValueArgument(1, value)
                        }
                    }
                )
            )
        }

    fun varargOf(elementType: IrType, elements: List<IrExpression>) = IrVarargImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        context.irBuiltIns.arrayClass.typeWith(elementType),
        elementType,
        elements
    )

    fun IrBuilderWithScope.callTypeOf(type: IrType) = irCall(typeOf).apply { putTypeArgument(0, type) }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        if (declaration.descriptor.isActual) {
            val expect = context.symbolTable.referenceFunction(
                declaration.descriptor.findExpects().single() as CallableDescriptor
            ).owner
            val methodAnnotation = expect.getAnnotation(Names.KrosstalkMethod)

            if (methodAnnotation != null) {
                val klass = (methodAnnotation.getValueArgument(0) as IrClassReference).symbol.owner as IrClass
                val scopes = if (methodAnnotation.valueArgumentsCount > 1)
                    (methodAnnotation.getValueArgument(1) as IrVararg).elements.map { (it as IrConst<String>).value }
                else
                    emptyList()

                val scopeProps = klass.scopeProps()

                scopes.forEach {
                    if (it !in scopeProps) {
                        messageCollector.report(
                            CompilerMessageSeverity.ERROR,
                            "No known attribute $it.  Must define a property of the same name with type ScopeHolder in ${klass.fqNameForIrSerialization} or a superclass/interface.",
                            methodAnnotation.location()
                        )
                    }
                }

                val initializer = addedInitalizers.getOrPut(klass.symbol) {
                    klass.addAnonymousInitializer {
                        body = DeclarationIrBuilder(context, klass.symbol).irBlockBody { }
                    }
                }

                initializer.body.apply {
                    DeclarationIrBuilder(context, initializer.symbol).apply {
                        statements += irCall(addMethod).apply {
                            putTypeArgument(0, declaration.returnType)
                            // Name
                            putValueArgument(0, declaration.name.asString().asConst())
                            // MethodTypes
                            putValueArgument(1,
                                irCall(MethodTypes.constructors.single()).apply {
                                    putValueArgument(
                                        0,
                                        mapOf(
                                            context.irBuiltIns.stringType,
                                            KType.defaultType,
                                            declaration.valueParameters.associate {
                                                it.name.asString().asConst() to callTypeOf(it.type)
                                            })
                                    )
                                    putValueArgument(1, callTypeOf(declaration.returnType))
                                    putValueArgument(
                                        2,
                                        declaration.dispatchReceiverParameter?.let { callTypeOf(it.type) } ?: nullConst(
                                            KType.defaultType
                                        ))
                                    putValueArgument(
                                        2,
                                        declaration.extensionReceiverParameter?.let { callTypeOf(it.type) }
                                            ?: nullConst(KType.defaultType))
                                }
                            )
                            // List of scopes
                            putValueArgument(2, varargOf(context.irBuiltIns.stringType, scopes.map { it.asConst() }))

                            // call function
                            //TODO lambda, extract things from map
                        }
                    }
                }

                log("Class", klass.dump(true))

                println()

            }
        }
        return super.visitSimpleFunction(declaration)
    }
}