package com.rnett.krosstalk.compiler

import com.rnett.krosstalk.compiler.naming.primaryConstructor
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


class KrosstalkMethodTransformer(
    override val context: IrPluginContext,
    val messageCollector: MessageCollector
) :
    IrElementTransformerVoidWithContext(), FileLoweringPass, HasContext {

    val stringType = context.irBuiltIns.stringType

    private lateinit var file: IrFile
    private lateinit var fileSource: String

    fun IrElement.location(): CompilerMessageLocation? {
        val beforeText = fileSource.substring(0, this.startOffset)
        val line = beforeText.count { it == '\n' } + 1
        val beforeLine = beforeText.substringBeforeLast('\n').length
        val offsetOnLine = this.startOffset - beforeLine
        return CompilerMessageLocation.create(file.path, line, offsetOnLine, null)
    }

    fun IrElement.reportError(
        message: String,
        level: CompilerMessageSeverity = CompilerMessageSeverity.ERROR
    ) = messageCollector.report(
        level,
        message,
        this.location()
    )

    override fun lower(irFile: IrFile) {
        file = irFile
        fileSource = File(irFile.path).readText()

        irFile.transformChildrenVoid()
    }

//    val KrosstalkResult by Names.KrosstalkResult.klass()
//    val ScopeHolder by Names.ScopeHolder.klass()
//    val KrosstalkClient by Names.KrosstalkClient.klass()
//    val addMethod by Names.addMethod.topLevelFunction()
//    val call by Names.call.topLevelFunction()
//    val getValueAsOrError by Names.getValueAsOrError.topLevelFunction()
//    val clientPlaceholder by Names.clientPlaceholder.topLevelFunction()
//
//    val typeOf by Names.typeOf.topLevelFunction()
//
//    val mapOf by getter {
//        context.referenceFunctions(Names.mapOf).singleOrNull {
//            it.owner.valueParameters.firstOrNull()?.isVararg == true
//        } ?: error("mapOf not found")
//    }
//
//    val setOf by getter {
//        context.referenceFunctions(Names.setOf).singleOrNull {
//            it.owner.valueParameters.firstOrNull()?.isVararg == true
//        } ?: error("setOf not found")
//    }
//
//    val listOf by getter {
//        context.referenceFunctions(Names.listOf).singleOrNull {
//            it.owner.valueParameters.firstOrNull()?.isVararg == true
//        } ?: error("listOf not found")
//    }
//
//    val to by Names.to.topLevelFunction()
//    val Pair by Names.Pair.klass()
//    val Map by Names.Map.klass()
//    val Set by Names.Set.klass()
//    val List by Names.List.klass()
//    val Iterable by Names.Iterable.klass()
//    val MethodTypes by Names.MethodTypes.klass()
//    val KType by Names.KType.klass()
//    val error by Names.error.topLevelFunction()
//    val Annotation by Names.Annotation.klass()
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

    fun IrClass.scopeProps() =
        properties.filter {
            it.name.asString() != "serialization" && it.name.asString() != "client" && it.name.asString() != "server" &&
                    context.typeTranslator.translateType(it.descriptor.type)
                        .isSubtypeOfClass(Krosstalk.ScopeHolder())
        }.map { it.name.identifier }.toSet()


    fun IrBuilderWithScope.mapOf(
        keyType: IrType,
        valueType: IrType,
        map: Map<IrExpression, IrExpression>
    ): IrCall =
        irCall(Kotlin.Collections.mapOf(), Kotlin.Collections.Map().typeWith(keyType, valueType)).apply {
            putTypeArgument(0, keyType)
            putTypeArgument(1, valueType)
            putValueArgument(0,
                varargOf(
                    Kotlin.Pair().typeWith(keyType, valueType),
                    map.map { (key, value) ->
                        irCall(Kotlin.to(), Kotlin.Pair().typeWith(keyType, valueType)).apply {
                            putTypeArgument(0, keyType)
                            putTypeArgument(1, valueType)
                            extensionReceiver = key
                            putValueArgument(0, value)
                        }
                    }
                )
            )
        }

    fun IrBuilderWithScope.setOf(type: IrType, items: Iterable<IrExpression>) =
        irCall(Kotlin.Collections.setOf(), Kotlin.Collections.Set().typeWith(type)).apply {
            putTypeArgument(0, type)
            putValueArgument(0, varargOf(type, items))
        }

    fun IrBuilderWithScope.listOf(type: IrType, items: Iterable<IrExpression>) =
        irCall(Kotlin.Collections.listOf(), Kotlin.Collections.List().typeWith(type)).apply {
            putTypeArgument(0, type)
            putValueArgument(0, varargOf(type, items))
        }

    fun IrBuilderWithScope.getValueOrError(
        methodName: String,
        map: IrExpression,
        type: IrType,
        key: String,
        default: IrSimpleFunction,
        nullError: String,
        typeError: String,
        keyType: IrType = stringType
    ) =
        irCall(Krosstalk.getValueAsOrError(), type).apply {
            putTypeArgument(0, keyType)
            putTypeArgument(1, type)

            extensionReceiver = map
            putValueArgument(0, methodName.asConst())
            putValueArgument(1, key.asConst())
            putValueArgument(2, lambdaArgument(default, type.makeNullable()))
            putValueArgument(3, nullError.asConst())
            putValueArgument(4, typeError.asConst())
        }

    fun IrBuilderWithScope.callTypeOf(type: IrType) =
        irCall(Kotlin.typeOf(), Kotlin.KType().typeWith()).apply { putTypeArgument(0, type) }


    override fun visitClassNew(declaration: IrClass): IrStatement {
        log("Class", declaration.dump(true))
        return super.visitClassNew(declaration)
    }

    val IrSimpleFunction.krosstalkArgumentNames
        get() = run {
            val validArgumentNames = mutableSetOf<String>()

            if (extensionReceiverParameter != null)
                validArgumentNames += extensionParameterKey

            if (dispatchReceiverParameter != null)
                validArgumentNames += instanceParameterKey

            validArgumentNames += valueParameters.map { it.name.asString() }

            validArgumentNames.toSet()
        }

    val IrSimpleFunction.validEndpointParameters
        get() = krosstalkArgumentNames + listOf(
            prefixKey,
            methodNameKey
        )

    fun checkAnnotations(
        declaration: IrSimpleFunction,
        annotations: KrosstalkAnnotations,
        klass: IrClass,
        isClientOnly: Boolean
    ) {
        if (!isClientOnly) {
            annotations.forEach {
                if (it.clientOnly) {
                    declaration.reportError("Can't use Krosstalk annotation $it on Krosstalk methods with server implementations.  Krosstalk object must be a non-actual client.")
                }
            }
        }

        val requiredScopes = annotations.RequiredScopes?.scopes ?: emptySet()
        val optionalScopes = annotations.OptionalScopes?.scopes ?: emptySet()

        val scopeProps = klass.scopeProps()

        (requiredScopes + optionalScopes).forEach {
            if (it !in scopeProps) {
                declaration.reportError(
                    "No known scope $it.  Must define a property of the same name with type ScopeHolder in ${klass.fqNameForIrSerialization} or a superclass/interface."
                )
            }
        }

        optionalScopes.forEach {
            if (it in requiredScopes)
                declaration.reportError("Scope $it is both optional and required.  This is illegal.")
        }


        if (annotations.NullOn != null && !declaration.returnType.isNullable())
            messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "Can't use NullOn annotation on function with non-nullable return type.",
                declaration.location()
            )


        val endpoint = annotations.KrosstalkEndpoint?.endpoint ?: defaultEndpoint

        val setEndpoint = annotations.KrosstalkEndpoint != null

        val requireEmptyBody = annotations.EmptyBody != null
        val minimizeBody = annotations.MinimizeBody != null || requireEmptyBody

        //TODO allow emptybody without endpoint for no args?
        if (minimizeBody && !setEndpoint) {
            if (requireEmptyBody)
                messageCollector.report(
                    CompilerMessageSeverity.ERROR,
                    "Can't use EmptyBody annotation without specifying an endpoint using KrosstalkEndpoint",
                    declaration.location()
                )
            else
                messageCollector.report(
                    CompilerMessageSeverity.ERROR,
                    "Can't use MinimizeBody annotation without specifying an endpoint using KrosstalkEndpoint",
                    declaration.location()
                )
        }

        val usedArgumentNames = mutableSetOf<String>()

        if (setEndpoint) {
            keyRegex.findAll(endpoint).forEach {
                val name = it.groupValues[1]
                if (name !in declaration.validEndpointParameters) {
                    when (name) {
                        extensionParameterKey -> declaration.reportError(
                            "Used $extensionParameterKey in endpoint template, but method does not have an extension receiver"
                        )
                        instanceParameterKey -> declaration.reportError(
                            "Used $instanceParameterKey in endpoint template, but method does not have an instance/dispatch receiver"
                        )
                        else -> declaration.reportError(
                            "Used $name in endpoint template, but method does not have a parameter named $name"
                        )
                    }
                } else {
                    usedArgumentNames += name
                }
            }
        }

        if (requireEmptyBody) {
            val leftovers = declaration.krosstalkArgumentNames - usedArgumentNames
            if (leftovers.isNotEmpty())
                declaration.reportError(
                    "EmptyBody is specified, but not all arguments are used in endpoint: missing $leftovers"
                )
        }

        if (annotations.NullOn != null && annotations.ExplicitResult != null)
            declaration.reportError("Can't use @NullOn and @ExplicitResult on the same method.")

        if (annotations.ExplicitResult != null) {
            if (declaration.returnType.classOrNull != Krosstalk.KrosstalkResult())
                declaration.reportError("Must have a return type of KrosstalkResult to use @ExplicitResult.")
        }

    }

    /**
     * Register a method in the krosstalk object's init blocks.
     */
    fun addMethodToClass(
        declaration: IrSimpleFunction,
        annotations: KrosstalkAnnotations,
        klass: IrClass
    ) {


        val initializer = addedInitalizers.getOrPut(klass.symbol) {
            klass.addAnonymousInitializer {
                body = DeclarationIrBuilder(context, this.symbol).irBlockBody {
                }
            }
        }

        initializer.body.apply {
            initializer.newBuilder {
                statements += irCall(Krosstalk.addMethod()).apply {
                    dispatchReceiver = klass.thisReceiver?.let { irGet(it) }
                    putTypeArgument(0, declaration.returnType)

                    var valueArguments = 0
                    fun addValueArgument(argument: IrExpression) = putValueArgument(valueArguments++, argument)


                    // Name
                    addValueArgument(declaration.name.asString().asConst())

                    // endpoint and method

                    addValueArgument(
                        (annotations.KrosstalkEndpoint?.endpoint ?: defaultEndpoint).asConst()
                    )
                    addValueArgument(
                        (annotations.KrosstalkEndpoint?.httpMethod ?: defaultEndpointMethod).asConst()
                    )

                    // MethodTypes
                    addValueArgument(
                        irCall(Krosstalk.MethodTypes().primaryConstructor!!).apply {
                            val parameterMap: MutableMap<IrExpression, IrExpression> =
                                declaration.valueParameters
                                    .associate {
                                        it.name.asString().asConst() to callTypeOf(it.type)
                                    }.toMutableMap()

                            declaration.extensionReceiverParameter?.let {
                                parameterMap[extensionParameterKey.asConst()] = callTypeOf(it.type)
                            }

                            declaration.dispatchReceiverParameter?.let {
                                parameterMap[instanceParameterKey.asConst()] = callTypeOf(it.type)
                            }

                            putValueArgument(
                                0,
                                mapOf(stringType, Kotlin.KType().typeWith(), parameterMap)
                            )
                            putValueArgument(1, callTypeOf(declaration.returnType))
                        }
                    )

                    // required scopes
                    addValueArgument(
                        setOf(
                            stringType,
                            annotations.RequiredScopes?.scopes.orEmpty().map { it.asConst() })
                    )
                    // optional scopes
                    addValueArgument(
                        setOf(
                            stringType,
                            annotations.OptionalScopes?.scopes.orEmpty().map { it.asConst() })
                    )

                    // leave out arguments
                    addValueArgument(
                        if (annotations.MinimizeBody != null || annotations.EmptyBody != null)
                            setOf(stringType, declaration.krosstalkArgumentNames.map { it.asConst() })
                        else
                            nullConst(Kotlin.Collections.Set().typeWith(stringType))
                    )

                    // nullOnResponse
                    addValueArgument(
                        setOf(
                            context.irBuiltIns.intType,
                            (annotations.NullOn?.responseCodes ?: emptySet()).map { it.asConst() }
                        )
                    )

                    // useExplicitResult
                    addValueArgument((annotations.ExplicitResult != null).asConst())

                    // includeStacktrace
                    addValueArgument(
                        (annotations.ExplicitResult?.includeStacktrace == true).asConst()
                    )

                    // annotations

//                    putValueArgument(10,
//                        mapOf(context.irBuiltIns.kClassClass.typeWith(Annotation.defaultType),
//                            Map.typeWith(stringType, context.irBuiltIns.anyType.makeNullable()),
//                            annotations.filter {
//                                it.symbol.owner.parentAsClass.fqNameForIrSerialization.isChildOf(
//                                    annotationsPackage
//                                )
//                            }.associate {
//                                val klass = it.symbol.owner.parentAsClass
//
//                                val arguments = it.getArgumentsWithIr().toMap()
//
//                                val argMap = it.symbol.owner.valueParameters.associate {
//                                    it.name.asString() to (
//                                            arguments[it]?.deepCopyWithSymbols()
//                                            //TODO can't get default values, they are IrErrorExpressions
////                                                ?: it.defaultValue?.expression?.deepCopyWithSymbols()
//                                                ?: if (it.isVararg)
//                                                    varargOf(it.varargElementType!!, emptyList())
//                                                else
//                                                    null // error("No default but not set?") //TODO handle empty varargs
//                                            )
//                                }.filterValues { it != null } as Map<String, IrExpression>
//
//                                IrClassReferenceImpl(
//                                    UNDEFINED_OFFSET,
//                                    UNDEFINED_OFFSET,
//                                    context.irBuiltIns.kClassClass.typeWith(Annotation.defaultType),
//                                    klass.symbol,
//                                    klass.defaultType
//                                ) to
//                                        mapOf(
//                                            stringType,
//                                            context.irBuiltIns.anyType.makeNullable(),
//                                            argMap.mapKeys { it.key.asConst() })
//                            })
//                    )

                    // call function

                    val lambda = buildLambda(declaration.returnType, { isSuspend = true }) {
                        newBuilder {
                            val args = addValueParameter {
                                name = Name.identifier("arguments")
                                type = IrSimpleTypeImpl(
                                    Kotlin.Collections.Map(),
                                    false,
                                    listOf(stringType as IrTypeBase, IrStarProjectionImpl),
                                    emptyList()
                                )
                            }

                            body = irJsExperBody(irCall(declaration.symbol).apply {
                                declaration.valueParameters.forEach {
                                    putValueArgument(
                                        it.index,
                                        getValueOrError(
                                            declaration.name.asString(),
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
                                        declaration.name.asString(),
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
                                        declaration.name.asString(),
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
                        }
                    }

                    addValueArgument(lambdaArgument(lambda))

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
            function.body = irJsExperBody(irCall(Krosstalk.call(), function.returnType).apply {
                extensionReceiver = irGetObject(krosstalkClass.symbol)
                putTypeArgument(0, function.returnType)
                putTypeArgument(1, krosstalkClass.defaultType.makeNotNull())

                val clientScopeType =
                    krosstalkClass.superTypes.single { it.classifierOrNull == Krosstalk.KrosstalkClient() }
                        .cast<IrSimpleType>().arguments.single().typeOrNull!!

                putTypeArgument(2, clientScopeType)

                val argumentsMap =
                    function.valueParameters.associate { it.name.asString().asConst() to irGet(it) }
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
                        stringType,
                        context.irBuiltIns.anyType.makeNullable(),
                        argumentsMap
                    )
                )
            })
        }
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {

//        log("Function", declaration.dump(true))

        if (declaration.isExpect)
            return super.visitSimpleFunction(declaration)

        val annotations: List<IrConstructorCall>
        val expectKrosstalk: Boolean

        if (declaration.descriptor.isActual) {
            val expect = context.symbolTable.referenceFunction(
                declaration.descriptor.findExpects().single() as CallableDescriptor
            ).owner


            if (expect.hasAnnotation(Krosstalk.Annotations.KrosstalkMethod())) {
                annotations = expect.annotations + declaration.annotations
                expectKrosstalk = true

                if (declaration.hasAnnotation(Krosstalk.Annotations.KrosstalkMethod()))
                    messageCollector.report(
                        CompilerMessageSeverity.ERROR,
                        "Can't define a method as a Krosstalk more than once.  It was defined on both the expect and actual declarations.",
                        declaration.location()
                    )

                KrosstalkAnnotations(declaration.annotations).forEach {
                    if (it.mustMatch) {
                        messageCollector.report(
                            CompilerMessageSeverity.ERROR,
                            "Can't configure Krosstalk annotation ${it.annotationName} on the actual method when the expect declaration is a Krosstalk method.",
                            declaration.location()
                        )
                    }
                }

            } else {
                annotations = declaration.annotations
                expectKrosstalk = false
            }
        } else {
            annotations = declaration.annotations
            expectKrosstalk = false
        }

        val methodAnnotation = annotations.getAnnotation(Krosstalk.Annotations.KrosstalkMethod.fqName)
            ?: return super.visitSimpleFunction(declaration)

        val krosstalkAnnotations = KrosstalkAnnotations(annotations)

        if (!declaration.isSuspend)
            messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "Krosstalk methods must be suspend",
                declaration.location()
            )

        val klass =
            (methodAnnotation.getValueArgument(0) as IrClassReference).symbol.owner as IrClass

        if (!klass.isObject)
            messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "Used Krosstalk class must be an object, ${klass.fqNameForIrSerialization} was not",
                declaration.location()
            )

        //TODO I'd like to allow client only things for an expect krosstalk where all actuals are clients.  Unsure if possible
        val isClient = klass.isSubclassOf(Krosstalk.KrosstalkClient().owner)
        val isClientOnly = !expectKrosstalk && isClient

        checkAnnotations(declaration, krosstalkAnnotations, klass, isClientOnly)

        addMethodToClass(declaration, krosstalkAnnotations, klass)

        if (klass.isSubclassOf(Krosstalk.KrosstalkClient().owner)) {
            //TODO can I use no body and suppress the error?  But then I need an ide plugin
            fun bodyError() {
                messageCollector.report(
                    CompilerMessageSeverity.ERROR,
                    "Krosstalk client side methods should be only empty or ${
                        Krosstalk.clientPlaceholder.fqName.shortName().asString()
                    }",
                    declaration.location()
                )
            }

            val body = declaration.body
            if (body is IrExpressionBody) {
                val expr = body.expression
                if (!(expr is IrCall && expr.symbol == Krosstalk.clientPlaceholder()))
                    bodyError()

            } else if (body is IrBlockBody) {
                if (body.statements.size > 1)
                    bodyError()
                else if (body.statements.isNotEmpty()) {
                    val ret = body.statements[0]
                    if (ret is IrReturn) {
                        val expr = ret.value
                        if (!(expr is IrCall && expr.symbol == Krosstalk.clientPlaceholder()))
                            bodyError()
                    } else if (!(ret is IrCall && ret.symbol == Krosstalk.clientPlaceholder()))
                        bodyError()
                }
            }

            addCallMethodBody(klass, declaration)
        }
        return super.visitSimpleFunction(declaration)
    }
}