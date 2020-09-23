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
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.types.impl.IrTypeBase
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.multiplatform.findExpects
import org.jetbrains.kotlin.utils.addToStdlib.cast
import java.io.File

const val instanceParameterKey = "\$instance"
const val extensionParameterKey = "\$extension"
const val methodNameKey = "\$name"
const val prefixKey = "\$prefix"

private val valueRegex = Regex("\\{([^}]+?)\\}")

class KrosstalkMethodTransformer(override val context: IrPluginContext, val messageCollector: MessageCollector) :
    IrElementTransformerVoidWithContext(), FileLoweringPass, HasContext {

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

    val ScopeHolder by Names.ScopeHolder.klass()
    val KrosstalkClient by Names.KrosstalkClient.klass()
    val addMethod by Names.addMethod.topLevelFunction()
    val call by Names.call.topLevelFunction()
    val OptionalNone by Names.OptionalNone.klass()
    val OptionalSome by Names.OptionalSome.klass()
    val getValueAsOrError by Names.getValueAsOrError.topLevelFunction()
    val clientPlaceholder by Names.clientPlaceholder.topLevelFunction()

    val typeOf by Names.typeOf.topLevelFunction()

    val mapOf by getter {
        context.referenceFunctions(Names.mapOf).singleOrNull {
            it.owner.valueParameters.firstOrNull()?.isVararg == true
        } ?: error("mapOf not found")
    }

    val setOf by getter {
        context.referenceFunctions(Names.setOf).singleOrNull {
            it.owner.valueParameters.firstOrNull()?.isVararg == true
        } ?: error("setOf not found")
    }
    val to by Names.to.topLevelFunction()
    val Pair by Names.Pair.klass()
    val Map by Names.Map.klass()
    val Set by Names.Set.klass()
    val Iterable by Names.Iterable.klass()
    val MethodTypes by Names.MethodTypes.klass()
    val KType by Names.KType.klass()
    val error by Names.error.topLevelFunction()
//    val error by Names.error.function()

    fun IrBuilderWithScope.constStringProperty(name: FqName) =
        this@KrosstalkMethodTransformer.context.referenceProperties(name).single().owner.let {
            assert(it.isConst) { "Expected $name to be a const" }
            it.backingField!!.initializer!!.expression as IrConst<String>
        }.value

    // constants
    //TODO use these instead of hardcoded constants (js is broken, see slack)
//    val IrBuilderWithScope.instanceParameterKey get() = constStringProperty(Names.instanceParameterKey)
//    val IrBuilderWithScope.extensionParameterKey get() = constStringProperty(Names.extensionParameterKey)
//    val IrBuilderWithScope.methodNameKey get() = constStringProperty(Names.methodNameKey)
//    val IrBuilderWithScope.prefixKey get() = constStringProperty(Names.prefixKey)


    val addedInitalizers = mutableMapOf<IrClassSymbol, IrAnonymousInitializer>()

    fun IrClass.scopeProps() = properties.filter {
        context.typeTranslator.translateType(it.descriptor.type).isSubtypeOfClass(ScopeHolder)
    }.map { it.name.identifier }.toSet()

    fun IrBuilderWithScope.mapOf(keyType: IrType, valueType: IrType, map: Map<IrExpression, IrExpression>): IrCall {
        return irCall(mapOf, Map.typeWith(keyType, valueType)).apply {
            putTypeArgument(0, keyType)
            putTypeArgument(1, valueType)
            putValueArgument(0,
                varargOf(
                    Pair.typeWith(keyType, valueType),
                    map.map { (key, value) ->
                        irCall(to, Pair.typeWith(keyType, valueType)).apply {
                            putTypeArgument(0, keyType)
                            putTypeArgument(1, valueType)
                            extensionReceiver = key
                            putValueArgument(0, value)
                        }
                    }
                )
            )
        }
    }

    fun IrBuilderWithScope.setOf(type: IrType, items: Iterable<IrExpression>) =
        irCall(setOf, Set.typeWith(type)).apply {
            putTypeArgument(0, type)
            putValueArgument(0, varargOf(type, items))
        }

    fun IrBuilderWithScope.getValueOrError(
        map: IrExpression,
        type: IrType,
        key: String,
        default: IrSimpleFunction,
        nullError: String,
        typeError: String,
        keyType: IrType = context.irBuiltIns.stringType
    ) = irCall(getValueAsOrError, type).apply {
        putTypeArgument(0, keyType)
        putTypeArgument(1, type)

        extensionReceiver = map
        putValueArgument(0, key.asConst())
        putValueArgument(1, lambdaArgument(default, type.makeNullable()))
        putValueArgument(2, nullError.asConst())
        putValueArgument(3, typeError.asConst())
    }

    fun IrBuilderWithScope.callTypeOf(type: IrType) =
        irCall(typeOf, KType.typeWith()).apply { putTypeArgument(0, type) }

    override fun visitClassNew(declaration: IrClass): IrStatement {
        log("Class", declaration.dump(true))
        return super.visitClassNew(declaration)
    }

    /**
     * Register a method in the krosstalk object's init blocks.
     */
    fun addMethodToClass(
        declaration: IrSimpleFunction,
        expectDeclaration: IrFunction,
        methodAnnotation: IrConstructorCall,
        klass: IrClass
    ) {

        val requiredScopes = expectDeclaration.getAnnotation(Names.RequiredScopes)?.let {
            if (it.valueArgumentsCount > 0)
                (it.getValueArgument(0) as IrVararg).elements.map { (it as IrConst<String>).value }.toSet()
            else
                emptySet()
        } ?: emptySet()

        val optionalScopes = expectDeclaration.getAnnotation(Names.OptionalScopes)?.let {
            if (it.valueArgumentsCount > 0)
                (it.getValueArgument(0) as IrVararg).elements.map { (it as IrConst<String>).value }.toSet()
            else
                emptySet()
        } ?: emptySet()

        val scopeProps = klass.scopeProps()

        (requiredScopes + optionalScopes).forEach {
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
                body = DeclarationIrBuilder(context, this.symbol).irBlockBody {
                }
            }
        }

        initializer.body.apply {
            initializer.newBuilder {
                statements += irCall(addMethod).apply {
                    dispatchReceiver = klass.thisReceiver?.let { irGet(it) }
                    putTypeArgument(0, declaration.returnType)
                    // Name
                    putValueArgument(0, declaration.name.asString().asConst())

                    // endpoint and method

                    val endpointAnnotation = expectDeclaration.getAnnotation(Names.KrosstalkEndpoint)

                    val endpoint = endpointAnnotation?.let {
                        it.getValueArgument(0)!! as IrConst<String>
                    }?.value ?: "{$prefixKey}/{$methodNameKey}"

                    val method = endpointAnnotation?.let {
                        it.getValueArgument(1) as IrConst<String>?
                    }?.value ?: "POST"

                    val validNames = mutableSetOf(prefixKey, methodNameKey)

                    if (declaration.extensionReceiverParameter != null)
                        validNames += extensionParameterKey

                    if (declaration.dispatchReceiverParameter != null)
                        validNames += instanceParameterKey

                    validNames += declaration.valueParameters.map { it.name.asString() }

                    if (endpointAnnotation != null) {
                        valueRegex.findAll(endpoint).forEach {
                            val name = it.groupValues[1]
                            if (name !in validNames) {
                                when (name) {
                                    extensionParameterKey -> messageCollector.report(
                                        CompilerMessageSeverity.ERROR,
                                        "Used $extensionParameterKey in endpoint template, but method does not have an extension receiver",
                                        endpointAnnotation.location()
                                    )
                                    instanceParameterKey -> messageCollector.report(
                                        CompilerMessageSeverity.ERROR,
                                        "Used $instanceParameterKey in endpoint template, but method does not have an instance/dispatch receiver",
                                        endpointAnnotation.location()
                                    )
                                    else -> messageCollector.report(
                                        CompilerMessageSeverity.ERROR,
                                        "Used $name in endpoint template, but method does not have a parameter named $name",
                                        endpointAnnotation.location()
                                    )
                                }
                            }
                        }
                    }

                    putValueArgument(1, endpoint.asConst())
                    putValueArgument(2, method.asConst())

                    // MethodTypes
                    putValueArgument(3,
                        irCall(MethodTypes.constructors.single { it.owner.isPrimary }).apply {
                            val parameterMap: MutableMap<IrExpression, IrExpression> = declaration.valueParameters
                                .associate { it.name.asString().asConst() to callTypeOf(it.type) }.toMutableMap()

                            declaration.extensionReceiverParameter?.let {
                                parameterMap[extensionParameterKey.asConst()] = callTypeOf(it.type)
                            }

                            declaration.dispatchReceiverParameter?.let {
                                parameterMap[instanceParameterKey.asConst()] = callTypeOf(it.type)
                            }

                            putValueArgument(
                                0,
                                mapOf(
                                    context.irBuiltIns.stringType,
                                    KType.typeWith(),
                                    parameterMap
                                )
                            )
                            putValueArgument(1, callTypeOf(declaration.returnType))
                        }
                    )

                    if (declaration.name.asString() == "doAuthThing")
                        println()

                    // required scopes
                    putValueArgument(4, setOf(context.irBuiltIns.stringType, requiredScopes.map { it.asConst() }))
                    // optional scopes
                    putValueArgument(5, setOf(context.irBuiltIns.stringType, optionalScopes.map { it.asConst() }))

                    // call function

                    val lambda = buildLambda(declaration.returnType, { isSuspend = true }) {
                        newBuilder {
                            val args = addValueParameter {
                                name = Name.identifier("arguments")
                                type = IrSimpleTypeImpl(
                                    Map,
                                    false,
                                    listOf(context.irBuiltIns.stringType as IrTypeBase, IrStarProjectionImpl),
                                    emptyList()
                                )
                            }
//                                    if(Platform.platform == Platform.JS) {
//                                        body = this.irBlockBody { +irReturn(irCall(error, declaration.returnType).apply {
//                                            putValueArgument(0, "Dynamic calling not supported in JS".asConst())
//                                        }) }
//                                    } else {
                            body = irJsExperBody(irCall(declaration.symbol).apply {
                                declaration.valueParameters.forEach {
                                    putValueArgument(
                                        it.index,
                                        getValueOrError(
                                            irGet(args),
                                            it.type,
                                            it.name.asString(),
                                            buildLambda(it.type.makeNullable()) {
                                                body = it.defaultValue?.deepCopyWithSymbols()
                                                    ?.let { irJsExperBody(it) }
                                                    ?: irJsExperBody(nullConst(it.type.makeNullable()))
                                            },
                                            "No argument for ${it.name}, but it was required",
                                            "Argument for ${it.name} was type \$type, but the parameter is of type \$required"
                                        )
                                    )
                                }

                                declaration.extensionReceiverParameter?.let {
                                    extensionReceiver = getValueOrError(
                                        irGet(args),
                                        it.type,
                                        extensionParameterKey,
                                        buildLambda(it.type.makeNullable()) {
                                            body = irJsExperBody(nullConst(it.type.makeNullable()))
                                        },
                                        "No extension receiver argument, but it was required",
                                        "Extension receiver argument was type \$type, but parameter is of type \$required"
                                    )
                                }

                                declaration.dispatchReceiverParameter?.let {
                                    dispatchReceiver = getValueOrError(
                                        irGet(args),
                                        it.type,
                                        instanceParameterKey,
                                        buildLambda(it.type.makeNullable()) {
                                            body = irJsExperBody(nullConst(it.type.makeNullable()))
                                        },
                                        "No instance receiver argument, but it was required",
                                        "Instance receiver argument was type \$type, but parameter is of type \$required"
                                    )
                                }

                            })
//                                    }
                        }
                    }

                    putValueArgument(6, lambdaArgument(lambda))

                }
            }
        }

        initializer.patchDeclarationParents(klass)
    }

    /**
     * Replace a krosstalk function's body with Krosstalk.call(...)
     */
    fun addCallMethodBody(krosstalkClass: IrClass, function: IrSimpleFunction) {
        function.newBuilder {
            function.body = irJsExperBody(irCall(call, function.returnType).apply {
                extensionReceiver = irGetObject(krosstalkClass.symbol)
                putTypeArgument(0, function.returnType)
                putTypeArgument(1, krosstalkClass.defaultType.makeNotNull())

                val clientScopeType = krosstalkClass.superTypes.single { it.classifierOrNull == KrosstalkClient }
                    .cast<IrSimpleType>().arguments.single().typeOrNull!!

                putTypeArgument(2, clientScopeType)

                val argumentsMap = function.valueParameters.associate { it.name.asString().asConst() to irGet(it) }
                    .toMutableMap<IrExpression, IrExpression>()

                function.extensionReceiverParameter?.let {
                    argumentsMap[extensionParameterKey.asConst()] = irGet(it)
                }
                function.dispatchReceiverParameter?.let {
                    argumentsMap[instanceParameterKey.asConst()] = irGet(it)
                }

                putValueArgument(0, function.name.asString().asConst())
                putValueArgument(
                    1, mapOf(
                        context.irBuiltIns.stringType,
                        context.irBuiltIns.anyType.makeNullable(),
                        argumentsMap
                    )
                )
            })
        }
    }

    //TODO support non-expect (i.e. client only for existing server)
    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {

//        log("Function", declaration.dump(true))

        if (declaration.descriptor.isActual) {
            val expect = context.symbolTable.referenceFunction(
                declaration.descriptor.findExpects().single() as CallableDescriptor
            ).owner
            val methodAnnotation = expect.getAnnotation(Names.KrosstalkMethod)

            if (methodAnnotation != null) {
                if (!declaration.isSuspend)
                    messageCollector.report(
                        CompilerMessageSeverity.ERROR,
                        "Krosstalk methods must be suspend",
                        declaration.location()
                    )

                val klass = (methodAnnotation.getValueArgument(0) as IrClassReference).symbol.owner as IrClass

                if (!klass.isObject)
                    messageCollector.report(CompilerMessageSeverity.ERROR,
                        "Krosstalk class must be an object",
                        declaration.location())

                addMethodToClass(declaration, expect, methodAnnotation, klass)

                if (klass.isSubclassOf(KrosstalkClient.owner)) {
                    //TODO can I use no body and suppress the error?  But then I need an ide plugin
                    fun bodyError() {
                        messageCollector.report(CompilerMessageSeverity.ERROR,
                            "Krosstalk client side methods should be only empty or ${Names.clientPlaceholder.shortName().asString()}",
                            declaration.location())
                    }

                    val body = declaration.body
                    if (body is IrExpressionBody) {
                        val expr = body.expression
                        if (!(expr is IrCall && expr.symbol == clientPlaceholder))
                            bodyError()

                    } else if (body is IrBlockBody) {
                        if (body.statements.size > 1)
                            bodyError()
                        else if (body.statements.isNotEmpty()) {
                            val ret = body.statements[0]
                            if (ret is IrReturn) {
                                val expr = ret.value
                                if (!(expr is IrCall && expr.symbol == clientPlaceholder))
                                    bodyError()
                            } else if (!(ret is IrCall && ret.symbol == clientPlaceholder))
                                bodyError()
                        }
                    }

                    addCallMethodBody(klass, declaration)
                }

            }
        }
        return super.visitSimpleFunction(declaration)
    }
}