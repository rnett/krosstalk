package com.rnett.krosstalk.compiler

import com.rnett.krosstalk.defaultEndpoint
import com.rnett.krosstalk.defaultEndpointMethod
import com.rnett.krosstalk.endpoint.Endpoint
import com.rnett.krosstalk.extensionParameter
import com.rnett.krosstalk.instanceParameter
import com.rnett.krosstalk.methodName
import com.rnett.krosstalk.prefix
import com.rnett.plugin.ir.IrTransformer
import com.rnett.plugin.ir.addAnonymousInitializer
import com.rnett.plugin.ir.irJsExprBody
import com.rnett.plugin.ir.raiseTo
import com.rnett.plugin.ir.typeArgument
import com.rnett.plugin.ir.withDispatchReceiver
import com.rnett.plugin.ir.withExtensionReceiver
import com.rnett.plugin.ir.withTypeArguments
import com.rnett.plugin.ir.withValueArguments
import com.rnett.plugin.naming.isClassifierOf
import com.rnett.plugin.stdlib.Kotlin
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irIfNull
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.types.impl.IrTypeBase
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.types.isSubtypeOf
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.multiplatform.findExpects
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set


class KrosstalkMethodTransformer(
    context: IrPluginContext,
    messageCollector: MessageCollector,
) : IrTransformer(context, messageCollector) {

    val stringType = context.irBuiltIns.stringType

    val addedInitalizers = mutableMapOf<IrClassSymbol, IrAnonymousInitializer>()

    fun IrBuilderWithScope.getValueOrError(
        methodName: String,
        map: IrExpression,
        type: IrType,
        key: String,
        default: IrSimpleFunction,
        nullError: String,
        typeError: String,
        keyType: IrType = stringType,
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

    inner class KrosstalkClass(val declaration: IrClass) {
        val scopes by lazy {
            declaration.declarations.filterIsInstance<IrClass>()
                .filter { it.isNonCompanionObject && it.isSubclassOf(Krosstalk.Scope) }
        }

        val isClient by lazy { declaration.isSubclassOf(Krosstalk.KrosstalkClient) }
        val isServer by lazy { declaration.isSubclassOf(Krosstalk.KrosstalkServer) }

        private fun reportError(message: String) = messageCollector.reportError(message, declaration)

        private var checked = false

        fun check() {
            if (checked)
                return

            if (!declaration.isObject || declaration.isAnonymousObject || declaration.isCompanion) {
                reportError("Krosstalk objects must be top level objects (not companions or anonymous objects)")
            }

            if (isClient && isServer && !declaration.isExpect)
                reportError("The same object can't be a Krosstalk Client and Server")

            scopes.forEach {
                if (isClient) {
                    val scopeType = declaration.defaultType.raiseTo(Krosstalk.KrosstalkClient()).typeArgument(0)
                    if (!it.defaultType.isSubtypeOf(scopeType, context.irBuiltIns))
                        messageCollector.reportError(
                            "All scopes in a Krosstalk Client object must extend the client's scope type ${scopeType.render()}, ${it.name} does not.",
                            it
                        )
                }
                if (isServer) {
                    val scopeType = declaration.defaultType.raiseTo(Krosstalk.KrosstalkServer()).typeArgument(0)
                    if (!it.defaultType.isSubtypeOf(scopeType, context.irBuiltIns))
                        messageCollector.reportError(
                            "All scopes in a Krosstalk Server object must extend the server's scope type ${scopeType.render()}, ${it.name} does not.",
                            it
                        )
                }
            }
            checked = true
        }

        fun registerScopes() {
            declaration.addAnonymousInitializer {
                body = DeclarationIrBuilder(context, this.symbol).irBlockBody {
                    scopes.forEach {
                        +irCall(Krosstalk.Krosstalk.addScope)
                            .withDispatchReceiver(irGetObject(declaration.symbol))
                            .withValueArguments(irGetObject(it.symbol))
                    }
                }
            }
        }
    }

    inner class KrosstalkFunction(val declaration: IrSimpleFunction) {

        val scopes by lazy {
            declaration.valueParameters.filter { it.type.isClassifierOf(Krosstalk.ScopeInstance) }
                .associateWith { it.type.typeArgument(0).classOrNull!!.owner }
        }

        val requiredScopes by lazy {
            scopes.filterKeys { !it.type.isNullable() }
        }

        val optionalScopes by lazy {
            scopes.filterKeys { it.type.isNullable() }
        }

        val nonScopeValueParameters by lazy {
            declaration.valueParameters.filter { it !in scopes }
        }

        val allNonScopeParameters by lazy {
            nonScopeValueParameters + listOfNotNull(
                declaration.extensionReceiverParameter,
                declaration.dispatchReceiverParameter
            )
        }

        val hasArgs by lazy { allNonScopeParameters.isNotEmpty() }

        @OptIn(ObsoleteDescriptorBasedAPI::class)
        val expectDeclaration by lazy {
            if (declaration.descriptor.isActual) {
                val expect = context.symbolTable.referenceFunction(
                    declaration.descriptor.findExpects().single() as CallableDescriptor
                ).owner

                if (expect.hasAnnotation(Krosstalk.Annotations.KrosstalkMethod()))
                    expect
                else
                    null
            } else
                null
        }

        val annotations by lazy {
            KrosstalkAnnotations(expectDeclaration?.annotations.orEmpty() + declaration.annotations)
        }

        val actualKrosstalkClass: IrClass by lazy {
            val methodAnnotation = declaration.annotations.getAnnotation(Krosstalk.Annotations.KrosstalkMethod.fqName)
                ?: expectDeclaration?.annotations?.getAnnotation(Krosstalk.Annotations.KrosstalkMethod.fqName)
                ?: error("Function is not a krosstalk method, this should not have been called.")
            (methodAnnotation.getValueArgument(0) as IrClassReference).symbol.owner as IrClass
        }

        @OptIn(ObsoleteDescriptorBasedAPI::class)
        val krosstalkClass by lazy {
            if (declaration.isExpect && !actualKrosstalkClass.isExpect) {
                val expectClass = context.symbolTable.referenceClass(
                    actualKrosstalkClass.descriptor.findExpects().single() as ClassDescriptor
                ).owner
                return@lazy KrosstalkClass(expectClass)
            }

            KrosstalkClass(actualKrosstalkClass)
        }

        val endpointTemplate by lazy { annotations.KrosstalkEndpoint?.endpoint ?: defaultEndpoint }

        val endpoint by lazy { Endpoint.withoutStatic(endpointTemplate) }

        val setEndpoint by lazy { annotations.KrosstalkEndpoint != null }

        val requireEmptyBody by lazy { annotations.EmptyBody != null }
        val minimizeBody by lazy { annotations.MinimizeBody != null || requireEmptyBody }

        private var checked = false

        @OptIn(ExperimentalStdlibApi::class)
        fun check() {

            if (checked)
                return

            if (!declaration.isSuspend)
                messageCollector.reportError(
                    "Krosstalk methods must be suspend",
                    declaration
                )

            krosstalkClass.check()

            if (!krosstalkClass.isClient && !krosstalkClass.isServer && !krosstalkClass.declaration.isExpect) {
                messageCollector.reportError("Krosstalk class " +
                        krosstalkClass.declaration.kotlinFqName + " is not a server or client krosstalk, can't add methods",
                    krosstalkClass.declaration)
            }

            scopes.forEach { (param, cls) ->
                if (cls.isCompanion || cls.isAnonymousObject || !cls.isObject) {
                    messageCollector.reportError(
                        "Scope parameter ${param.name} has invalid scope type.  Scopes must be objects nested in the Krosstalk class",
                        param
                    )
                }
                if (cls.parentClassOrNull?.symbol != krosstalkClass.declaration.symbol && cls.parentClassOrNull?.symbol != actualKrosstalkClass.symbol) {
                    messageCollector.reportError(
                        "Scope parameter ${param.name} has invalid scope type.  Scopes must be objects nested in the Krosstalk class",
                        param
                    )
                }
            }

            declaration.valueParameters.forEach { param: IrValueParameter ->
                if (param.type.isSubclassOf(Krosstalk.ScopeInstance) && !param.type.isClassifierOf(Krosstalk.ScopeInstance)) {
                    messageCollector.reportError(
                        "Krosstalk method scope parameters should only be ScopeInstance or ScopeInstance?, not a subclass",
                        param
                    )
                }

                if (param.type.isClassifierOf(Krosstalk.ScopeInstance)) {
                    if (param.type.isNullable() && param.defaultValue == null) {
                        messageCollector.reportWarning(
                            "Optional scope ${param.name} with no default value, should add a null default.",
                            param
                        )
                    }
                }
            }

            (requiredScopes.toList() + optionalScopes.toList())
                .groupBy({ it.second }, { it.first })
                .filterValues { it.size > 1 }
                .forEach { (cls, params) ->
                    messageCollector.reportError(
                        "Can only have one param for each scope, but ${params.map { it.name }} all use ${cls.name}",
                        params.first()
                    )
                }

            if (expectDeclaration != null) {
//                val expectAnnotations = expectDeclaration!!.annotations.associateBy { it.symbol.owner.constructedClass }
                declaration.annotations.forEach {
                    //TODO support annotations on the actual declaration if they match the expect
                    val annotationClass = it.symbol.owner.constructedClass
                    if (annotationClass.hasAnnotation(Krosstalk.Annotations.TopLevelOnly())) {
                        messageCollector.reportError(
                            "Krosstalk annotation ${annotationClass.name} must only be specified at the top level expect function.",
                            it
                        )
                    }
                }
            }


            if (annotations.NullOn != null && !declaration.returnType.isNullable()) {
                messageCollector.reportError(
                    "Can't use NullOn annotation on function with non-nullable return type.",
                    declaration
                )
            }

            if (minimizeBody && !setEndpoint && hasArgs) {
                if (requireEmptyBody)
                    messageCollector.reportError(
                        "Can't use @EmptyBody annotation without specifying an endpoint using @KrosstalkEndpoint (there's nowhere to put the arguments)",
                        declaration
                    )
                else
                    messageCollector.reportError(
                        "Can't use @MinimizeBody annotation without specifying an endpoint using @KrosstalkEndpoint (there's nowhere to put the arguments)",
                        declaration
                    )
            }

            val paramNames = nonScopeValueParameters.map { it.name.asString() }.toSet()
            if (setEndpoint) {
                val neededParams = endpoint.allReferencedParameters()
                if (declaration.extensionReceiverParameter == null && extensionParameter in neededParams) {
                    messageCollector.reportError(
                        "Used parameter $extensionParameter in endpoint template, but method does not have an extension receiver",
                        declaration
                    )
                }
                if (declaration.dispatchReceiverParameter == null && instanceParameter in neededParams) {
                    messageCollector.reportError(
                        "Used parameter $instanceParameter in endpoint template, but method does not have an extension receiver",
                        declaration
                    )
                }
                neededParams.minus(setOf(extensionParameter, instanceParameter, methodName, prefix)).forEach {
                    if (it !in paramNames) {
                        messageCollector.reportError(
                            "Used parameter $it in endpoint template, but method does not have a non-scope value parameter named $it",
                            declaration
                        )
                    }
                }
            }

            //TODO optional support, see Endpoint.  I'd like optional to be nullable on client, and require a default on server
            //  default is already used if param isn't in map, null is used for endpoints, is this already done (except checks)?
            if (requireEmptyBody) {
                buildSet {
                    addAll(paramNames)

                    if (declaration.extensionReceiverParameter != null)
                        add(extensionParameter)

                    if (declaration.dispatchReceiverParameter != null)
                        add(instanceParameter)
                }.forEach {
                    if (!endpoint.hasWhenNotNull(it)) {
                        messageCollector.reportError(
                            "Required an empty body with @EmptyBody, but parameter $it is not guaranteed to be in the endpoint when it is non-null.",
                            declaration
                        )
                    }
                }
            }

            annotations.KrosstalkEndpoint?.httpMethod?.let { method ->
                if (method.toLowerCase() == "get" && hasArgs && !requireEmptyBody) {
                    messageCollector.reportError(
                        "Can't use HTTP GET method without either having no parameters (including receivers) or using @EmptyBody.",
                        declaration
                    )
                }
            }

            if (annotations.NullOn != null && annotations.ExplicitResult != null)
                messageCollector.reportError("Can't use @NullOn and @ExplicitResult on the same method.", declaration)

            if (annotations.ExplicitResult != null) {
                if (declaration.returnType.classOrNull != Krosstalk.KrosstalkResult())
                    messageCollector.reportError(
                        "Must have a return type of KrosstalkResult to use @ExplicitResult.",
                        declaration
                    )
            }

            val isPlaceholderBody = when (val body = declaration.body) {
                is IrExpressionBody -> {
                    body.expression.let { it is IrCall && it.symbol == Krosstalk.clientPlaceholder() }
                }
                is IrBlockBody -> {
                    body.statements.size == 1 && body.statements.single()
                        .let { it is IrCall && it.symbol == Krosstalk.clientPlaceholder() }
                }
                else -> {
                    false
                }
            }

            if (krosstalkClass.isClient && !isPlaceholderBody) {
                messageCollector.reportError(
                    "Krosstalk methods for client should have placeholder krosstalkCall() body.",
                    declaration
                )
            }

            if (isPlaceholderBody && !krosstalkClass.isClient) {
                messageCollector.reportError(
                    "Krosstalk method has placeholder body, but is not on a client Krosstalk.  The method should be implemented.",
                    declaration
                )
            }

            checked = true
        }

        fun IrBuilderWithScope.buildCallLambda(): IrSimpleFunction {
            if (!krosstalkClass.isServer) {
                return buildLambda(declaration.returnType, { isSuspend = true }) {
                    val exceptionConstructor = Krosstalk.KrosstalkException.CallFromClientSide().constructors.single()
                    irThrow(
                        irCallConstructor(exceptionConstructor, emptyList())
                            .withValueArguments(declaration.name.asString().asConst())
                    )
                }
            }

            return buildLambda(declaration.returnType, { isSuspend = true }) {
                withBuilder {
                    val args = addValueParameter {
                        name = Name.identifier("arguments")
                        type = IrSimpleTypeImpl(
                            Kotlin.Collections.Map(),
                            false,
                            listOf(stringType as IrTypeBase, IrStarProjectionImpl),
                            emptyList()
                        )
                    }

                    val scopes = addValueParameter {
                        name = Name.identifier("scopes")
                        type = Krosstalk.ImmutableWantedScopes.resolveTypeWith()
                    }

                    body = irJsExprBody(irCall(declaration.symbol).apply {
                        nonScopeValueParameters.forEach {
                            putValueArgument(
                                it.index,
                                getValueOrError(
                                    declaration.name.asString(),
                                    irGet(args),
                                    it.type,
                                    it.name.asString(),
                                    buildLambda(it.type.makeNullable()) {
                                        body = it.defaultValue?.deepCopyWithSymbols()
                                            ?.let { irJsExprBody(it.expression) }
                                            ?: irJsExprBody(nullConst(it.type.makeNullable()))
                                    },
                                    "No argument for ${it.name}, but it was required",
                                    "Argument for ${it.name} was type \$type, but the parameter is of type \$required"
                                )
                            )
                        }

                        requiredScopes.forEach { (param, cls) ->
                            val dataType = cls.defaultType.raiseTo(Krosstalk.ServerScope()).typeArgument(0)
                            val data = irCall(Krosstalk.ImmutableWantedScopes.get)
                                .withDispatchReceiver(irGet(scopes))
                                .withTypeArguments(cls.defaultType, dataType)
                                .withValueArguments(irGetObject(cls.symbol))
                            putValueArgument(param.index,
                                irCall(Krosstalk.createServerScopeInstance)
                                    .withExtensionReceiver(irGetObject(cls.symbol))
                                    .withTypeArguments(cls.defaultType, dataType)
                                    .withValueArguments(data)
                            )
                        }

                        optionalScopes.forEach { (param, cls) ->
                            val dataType = cls.defaultType.raiseTo(Krosstalk.ServerScope()).typeArgument(0)
                            val data = irCall(Krosstalk.ImmutableWantedScopes.get)
                                .withDispatchReceiver(irGet(scopes))
                                .withTypeArguments(cls.defaultType, dataType)
                                .withValueArguments(irGetObject(cls.symbol))

                            val value = stdlib.letExpr(data) {
                                irIfNull(param.type, irGet(it),
                                    irNull(),
                                    irCall(Krosstalk.createServerScopeInstance)
                                        .withDispatchReceiver(irGetObject(cls.symbol))
                                        .withTypeArguments(cls.defaultType, dataType)
                                        .withValueArguments(data)
                                )
                            }

                            putValueArgument(param.index, value)
                        }

                        declaration.extensionReceiverParameter?.let {
                            extensionReceiver = getValueOrError(
                                declaration.name.asString(),
                                irGet(args),
                                it.type,
                                extensionParameter,
                                buildLambda(it.type.makeNullable()) {
                                    body = irJsExprBody(nullConst(it.type.makeNullable()))
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
                                instanceParameter,
                                buildLambda(it.type.makeNullable()) {
                                    body = irJsExprBody(nullConst(it.type.makeNullable()))
                                },
                                "No instance receiver argument, but it was required",
                                "Instance receiver argument was type \$type, but parameter is of type \$required"
                            )
                        }

                    })
                }
            }
        }

        fun addToKrosstalkClass() {
            val initializer = addedInitalizers.getOrPut(krosstalkClass.declaration.symbol) {
                krosstalkClass.declaration.addAnonymousInitializer {
                    body = DeclarationIrBuilder(context, this.symbol).irBlockBody {
                    }
                }
            }

            initializer.body.apply {
                initializer.withBuilder {
                    statements += irCall(Krosstalk.Krosstalk.addMethod).apply {
                        dispatchReceiver = krosstalkClass.declaration.thisReceiver?.let { irGet(it) }
                        putTypeArgument(0, declaration.returnType)

                        var valueArguments = 0
                        fun addValueArgument(argument: IrExpression) = putValueArgument(valueArguments++, argument)

                        // Name
                        addValueArgument(declaration.name.asString().asConst())

                        // endpoint and method

                        addValueArgument(endpointTemplate.asConst())
                        addValueArgument(
                            (annotations.KrosstalkEndpoint?.httpMethod ?: defaultEndpointMethod).asConst()
                        )

                        // MethodTypes
                        addValueArgument(
                            irCall(Krosstalk.Serialization.MethodTypes().owner.primaryConstructor!!).apply {
                                val parameterMap: MutableMap<IrExpression, IrExpression> =
                                    nonScopeValueParameters
                                        .associate {
                                            it.name.asString().asConst() to stdlib.reflect.typeOf(it.type)
                                        }.toMutableMap()

                                declaration.extensionReceiverParameter?.let {
                                    parameterMap[extensionParameter.asConst()] = stdlib.reflect.typeOf(it.type)
                                }

                                declaration.dispatchReceiverParameter?.let {
                                    parameterMap[instanceParameter.asConst()] = stdlib.reflect.typeOf(it.type)
                                }

                                putValueArgument(
                                    0,
                                    stdlib.collections.mapOf(stringType, Kotlin.Reflect.KType().typeWith(), parameterMap)
                                )
                                putValueArgument(1, stdlib.reflect.typeOf(declaration.returnType))
                            }
                        )

                        // required scopes
                        addValueArgument(
                            stdlib.collections.setOf(
                                Krosstalk.Scope.resolveTypeWith(),
                                requiredScopes.values.map { irGetObject(it.symbol) }
                            )
                        )

                        // optional scopes
                        addValueArgument(
                            stdlib.collections.setOf(
                                Krosstalk.Scope.resolveTypeWith(),
                                optionalScopes.values.map { irGetObject(it.symbol) }
                            )
                        )

                        // leave out arguments
                        addValueArgument(
                            (annotations.MinimizeBody != null || annotations.EmptyBody != null).asConst()
                        )

                        // nullOnResponse
                        addValueArgument(
                            stdlib.collections.setOf(
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

                        // call function
                        // signature is (Map<String, *>, ImmutableWantedScopes)
                        val lambda = buildCallLambda()

                        addValueArgument(lambdaArgument(lambda))

                    }
                }
            }

            initializer.patchDeclarationParents(krosstalkClass.declaration)
        }

        fun addCallMethodBody() {
            if (!krosstalkClass.isClient)
                return

            declaration.withBuilder {
                declaration.body = irJsExprBody(irCall(Krosstalk.call(), declaration.returnType).apply {

                    extensionReceiver = irGetObject(krosstalkClass.declaration.symbol)
                    putTypeArgument(0, declaration.returnType)
                    putTypeArgument(1, krosstalkClass.declaration.defaultType.makeNotNull())

                    val clientScopeType =
                        krosstalkClass.declaration.defaultType.raiseTo(Krosstalk.KrosstalkClient())
                            .typeArgument(0)

                    putTypeArgument(2, clientScopeType)

                    val argumentsMap =
                        nonScopeValueParameters.associate { it.name.asString().asConst() to irGet(it) }
                            .toMutableMap<IrExpression, IrExpression>()

                    declaration.extensionReceiverParameter?.let {
                        argumentsMap[extensionParameter.asConst()] = irGet(it)
                    }
                    declaration.dispatchReceiverParameter?.let {
                        argumentsMap[instanceParameter.asConst()] = irGet(it)
                    }

                    val scopeList = mutableListOf<IrExpression>()

                    requiredScopes.forEach { (param, cls) ->
                        scopeList.add(
                            irCall(Krosstalk.instanceToAppliedScope)
                                .withExtensionReceiver(irGet(param))
                                .withTypeArguments(cls.defaultType, context.irBuiltIns.anyType.makeNullable())
                        )
                    }

                    optionalScopes.forEach { (param, cls) ->
                        scopeList.add(
                            irCall(Krosstalk.instanceToAppliedScope)
                                .withExtensionReceiver(irGet(param))
                                .withTypeArguments(cls.defaultType, context.irBuiltIns.anyType.makeNullable())
                        )
                    }

                    putValueArgument(0, declaration.name.asString().asConst())
                    putValueArgument(
                        1, stdlib.collections.mapOf(
                            stringType,
                            context.irBuiltIns.anyType.makeNullable(),
                            argumentsMap
                        )
                    )
                    putValueArgument(
                        2, stdlib.collections.listOfNotNull(
                            Krosstalk.ScopeInstance.resolveTypeWith(),
                            scopeList
                        )
                    )
                })
            }

            messageCollector.reportInfo("New body for ${declaration.name}:\n" + declaration.dump(true), null)
            messageCollector.reportInfo("New body for ${declaration.name}:\n" + declaration.dumpKotlinLike(), null)

        }
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
//        log("Function", declaration.dump(true))

        if (declaration.isExpect) {
            if (declaration.annotations.hasAnnotation(Krosstalk.Annotations.KrosstalkMethod.fqName))
                KrosstalkFunction(declaration).check()
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
            KrosstalkFunction(declaration).apply {
                check()
                addToKrosstalkClass()
                addCallMethodBody()
            }
        }
        return super.visitSimpleFunction(declaration)
    }

    //TODO can I do some parts w/ the expect class?  I.e. method registration.  I can't check the scope params though, or make the call body
    override fun visitClassNew(declaration: IrClass): IrStatement {
        if (declaration.defaultType.isSubtypeOf(Krosstalk.Krosstalk.resolveTypeWith(), context.irBuiltIns) && !declaration.isExpect) {
            if (declaration.isObject && !declaration.isCompanion && !declaration.isAnonymousObject) {
                KrosstalkClass(declaration).apply {
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