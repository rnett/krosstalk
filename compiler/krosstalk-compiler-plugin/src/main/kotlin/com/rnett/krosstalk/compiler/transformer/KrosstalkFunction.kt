package com.rnett.krosstalk.compiler.transformer

import com.rnett.krosstalk.InternalKrosstalkApi
import com.rnett.krosstalk.KrosstalkPluginApi
import com.rnett.krosstalk.compiler.Krosstalk
import com.rnett.krosstalk.compiler.KrosstalkAnnotations
import com.rnett.krosstalk.defaultEndpoint
import com.rnett.krosstalk.defaultEndpointHttpMethod
import com.rnett.krosstalk.endpoint.Endpoint
import com.rnett.krosstalk.extensionReceiver
import com.rnett.krosstalk.instanceReceiver
import com.rnett.krosstalk.krosstalkPrefix
import com.rnett.krosstalk.methodName
import com.rnett.plugin.ir.HasContext
import com.rnett.plugin.ir.KnowsCurrentFile
import com.rnett.plugin.ir.addAnonymousInitializer
import com.rnett.plugin.ir.hasTypeArgument
import com.rnett.plugin.ir.irJsExprBody
import com.rnett.plugin.ir.irTypeOf
import com.rnett.plugin.ir.raiseTo
import com.rnett.plugin.ir.typeArgument
import com.rnett.plugin.ir.withDispatchReceiver
import com.rnett.plugin.ir.withExtensionReceiver
import com.rnett.plugin.ir.withTypeArguments
import com.rnett.plugin.ir.withValueArguments
import com.rnett.plugin.naming.ClassRef
import com.rnett.plugin.naming.isClassifierOf
import com.rnett.plugin.stdlib.Kotlin
import org.jetbrains.kotlin.backend.common.ir.allParameters
import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.backend.jvm.ir.isInCurrentModule
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irGetObjectValue
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.builders.parent
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.types.impl.IrTypeBase
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.types.isSubtypeOfClass
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.types.typeWithArguments
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isAnonymousObject
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.multiplatform.findExpects
import kotlin.reflect.KType

@OptIn(ExperimentalStdlibApi::class, InternalKrosstalkApi::class, KrosstalkPluginApi::class)
class KrosstalkFunction(val declaration: IrSimpleFunction, val methodTransformer: KrosstalkMethodTransformer) : HasContext by methodTransformer,
    KnowsCurrentFile by methodTransformer {

    fun IrType.isKrosstalkResultType() = this.isSubtypeOfClass(Krosstalk.Result.KrosstalkResult())

    val messageCollector = methodTransformer.messageCollector

    val hasArgs by lazy { allNonScopeParameters.filterNot { it.isSpecialParameter }.isNotEmpty() }

    val annotations by lazy {
        KrosstalkAnnotations(expectDeclaration?.annotations.orEmpty() + declaration.annotations)
    }

    val actualKrosstalkClass: IrClass by lazy {
        annotations.KrosstalkMethod!!.klass.symbol.owner as IrClass
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    val krosstalkClass by lazy {
        if (declaration.isExpect && !actualKrosstalkClass.isExpect) {
            val expectClass = context.symbolTable.referenceClass(
                actualKrosstalkClass.descriptor.findExpects().single() as ClassDescriptor
            ).owner
            return@lazy KrosstalkClass(expectClass, methodTransformer)
        }

        KrosstalkClass(actualKrosstalkClass, methodTransformer)
    }

    val endpointTemplate by lazy { annotations.KrosstalkEndpoint?.endpoint ?: defaultEndpoint }

    val endpoint by lazy { Endpoint.withoutStatic(endpointTemplate) }

    val setEndpoint by lazy { annotations.KrosstalkEndpoint != null }

    val requireEmptyBody by lazy { annotations.EmptyBody != null }

    private var checked = false

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

    private val expectParameters by lazy {
        expectDeclaration?.allParameters?.associateBy { it.name }
    }

    internal fun IrValueParameter.expect() = expectParameters?.getValue(name)

    internal fun IrType.expectableObject(): IrClass? {
        if (isNullable())
            return null

        val cls = classOrNull?.owner?.let { with(methodTransformer) { it.expect() } ?: it } ?: return null
        return if ((cls.isObject || cls.isCompanion) && !cls.isAnonymousObject)
            classOrNull?.owner
        else
            null
    }

    val parameters by lazy { declaration.allParameters.map { KrosstalkParameter(this, it) } }
    val paramMap by lazy { parameters.associateBy { it.declaration } }
    val IrValueParameter.param get() = paramMap.getValue(this)
    fun Iterable<IrValueParameter>.params() = map { it.param }
    val valueParameters by lazy { declaration.valueParameters.params() }

    fun Iterable<KrosstalkParameter>.withoutReceivers() =
        filter { !it.isExtensionReceiver && !it.isDispatchReceiver }

    val scopes by lazy {
        valueParameters.filter { it.isScope }
            .associateWith { it.scopeClass }
    }

    val requiredScopes by lazy {
        scopes.filterKeys { !it.isOptionalScope }
    }

    val optionalScopes by lazy {
        scopes.filterKeys { it.isOptionalScope }
    }

    val serverUrlParameter by lazy {
        parameters.firstOrNull { it.isServerURL }
    }

    val requestHeadersParameter by lazy {
        parameters.firstOrNull { it.isRequestHeaders }
    }

    val nonScopeValueParameters by lazy {
        valueParameters.filter { !it.isScope }
    }

    val nullableOptionalParameters by lazy {
        parameters.filter { it.isNullableOptional }
    }

    val serverDefaultParameters by lazy {
        parameters.filter { it.isServerDefaultOptional }
    }

    val allNonScopeParameters by lazy {
        nonScopeValueParameters + listOfNotNull(
            declaration.extensionReceiverParameter?.param,
            declaration.dispatchReceiverParameter?.param
        ).filterNot { it.isScope }
    }

    val objectParameters by lazy {
        if (annotations.PassObjects != null)
            emptySet()
        else
            parameters.filter { it.constantObject != null && it.krosstalkName !in endpoint.allReferencedParameters() }
                .toSet()
    }

    private fun IrType.unwrapClassifier(klass: ClassRef) = if (this.isClassifierOf(klass)) typeArgument(0) else this

    private fun IrType.unwrapKrosstalkResult() = if (this.isKrosstalkResultType()) {
        if (this.hasTypeArgument(0))
            typeArgument(0)
        else
            context.irBuiltIns.unitType
    } else {
        this
    }

    val returnDataType by lazy {
        val type = declaration.returnType

        if (type.isKrosstalkResultType()) {
            type.unwrapKrosstalkResult().unwrapClassifier(Krosstalk.WithHeaders)
        } else if (type.isClassifierOf(Krosstalk.WithHeaders)) {
            type.typeArgument(0).unwrapKrosstalkResult()
        } else
            type
    }

    val isWithHeaders by lazy {
        val type = declaration.returnType
        type.isClassifierOf(Krosstalk.WithHeaders) ||
                (type.isKrosstalkResultType() && type.hasTypeArgument(0) && type.typeArgument(0)
                    .isClassifierOf(Krosstalk.WithHeaders))
    }

    val isOuterWithHeaders by lazy {
        isWithHeaders && declaration.returnType.isClassifierOf(Krosstalk.WithHeaders)
    }

    val isInnerWithHeaders by lazy {
        isWithHeaders && !declaration.returnType.isClassifierOf(Krosstalk.WithHeaders)
    }

    val isKrosstalkResult by lazy {
        val type = declaration.returnType
        type.isKrosstalkResultType() ||
                (type.isClassifierOf(Krosstalk.WithHeaders) && type.typeArgument(0)
                    .isKrosstalkResultType())
    }

    val returnObject by lazy {
        if (annotations.PassObjects?.returnToo == true)
            return@lazy null
        returnDataType.expectableObject()
    }

    val placeholderCall by lazy {
        when (val body = declaration.body) {
            is IrExpressionBody -> {
                body.expression.let {
                    if (it is IrCall && it.isClientPlaceholder())
                        KrosstalkClientPlaceholder(it, context)
                    else
                        null
                }
            }
            is IrBlockBody -> {
                if (body.statements.size == 1) {
                    var statement = body.statements.single()
                    if(statement is IrReturn)
                        statement = (statement as IrReturn).value
                    statement.let {
                        if (it is IrCall && it.isClientPlaceholder())
                            KrosstalkClientPlaceholder(it, context)
                        else
                            null
                    }
                } else
                    null
            }
            else -> null
        }
    }

    fun check() {
        if (checked)
            return

        if (!declaration.isSuspend)
            messageCollector.reportError(
                "Krosstalk methods must be suspend",
                declaration
            )

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

        krosstalkClass.check()

        if(!krosstalkClass.declaration.isInCurrentModule()){
            messageCollector.reportError("Can't use a Krosstalk object from a different module!", declaration)
        }

        if (!krosstalkClass.isClient && !krosstalkClass.isServer && !krosstalkClass.declaration.isExpect) {
            messageCollector.reportError(
                "Krosstalk class " +
                        krosstalkClass.declaration.kotlinFqName + " is not a server or client krosstalk, can't add methods",
                krosstalkClass.declaration
            )
        }

        if (parameters.count { it.annotations.ServerURL != null } > 1) {
            messageCollector.reportError("Can only have one parameter marked with @ServerURL", declaration)
        }

        if (parameters.count { it.annotations.RequestHeaders != null } > 1) {
            messageCollector.reportError("Can only have one parameter marked with @RequestHeaders", declaration)
        }

        parameters.forEach { it.check() }

        (requiredScopes.toList() + optionalScopes.toList())
            .groupBy({ it.second }, { it.first })
            .filterValues { it.size > 1 }
            .forEach { (cls, params) ->
                messageCollector.reportError(
                    "Can only have one param for each scope, but \"${params.map { it.realName }}\" all use ${cls.name}",
                    params.first().declaration
                )
            }

        val allReturnTypeArgs = returnDataType.allTypes().groupBy { it.classOrNull }.mapValues { it.value.size }
        if (Krosstalk.WithHeaders() in allReturnTypeArgs) {
            messageCollector.reportError(
                "Can't use WithHeaders in return type except for top level or second level " +
                        "when used with KrosstalkResult (either order is valid).", declaration
            )
        }
        if (allReturnTypeArgs.keys.filterNotNull().any { it.defaultType.isKrosstalkResultType() }) {
            messageCollector.reportError(
                "Can't use KrosstalkResult in return type except for top level or second level " +
                        "when used with WithHeaders (either order is valid).", declaration
            )
        }

        if (declaration.returnType.isClassifierOf(Krosstalk.ServerDefault)) {
            messageCollector.reportError("Can't use ServerDefault as a return type", declaration)
        }

        if (declaration.extensionReceiverParameter?.type?.isClassifierOf(Krosstalk.ServerDefault) == true) {
            messageCollector.reportError("Can't use ServerDefault as a receiver type", declaration)
        }

        if (requireEmptyBody && !setEndpoint && hasArgs) {
            messageCollector.reportError(
                "Can't use @EmptyBody annotation without specifying an endpoint using @KrosstalkEndpoint (there's nowhere to put the arguments)",
                declaration
            )
        }

        val paramNames =
            nonScopeValueParameters.filter { !it.isSpecialParameter || it.isIgnored }.map { it.krosstalkName }.toSet()
        if (setEndpoint) {
            val neededParams = endpoint.allReferencedParameters()
            if (declaration.extensionReceiverParameter == null && extensionReceiver in neededParams) {
                messageCollector.reportError(
                    "Used parameter \"$extensionReceiver\" in endpoint template, but method does not have an extension receiver",
                    declaration
                )
            }
            if (declaration.dispatchReceiverParameter == null && instanceReceiver in neededParams) {
                messageCollector.reportError(
                    "Used parameter \"$instanceReceiver\" in endpoint template, but method does not have an instance receiver",
                    declaration
                )
            }
            neededParams.minus(setOf(extensionReceiver, instanceReceiver, methodName, krosstalkPrefix)).forEach {
                if (it !in paramNames) {
                    messageCollector.reportError(
                        "Used parameter \"$it\" in endpoint template, but method does not have a non-scope value parameter named \"$it\"",
                        declaration
                    )
                }
            }
            neededParams.mapNotNull { n -> parameters.firstOrNull { it.krosstalkName == n } }.forEach {
                if (it.isServerURL) {
                    messageCollector.reportError(
                        "Can't use @ServerURL parameter in endpoint, it will be prepended automatically",
                        declaration
                    )
                }
                if (it.isRequestHeaders) {
                    messageCollector.reportError("Can't use @RequestHeaders parameter in endpoint", declaration)
                }
            }
        }

        serverDefaultParameters.map { it.krosstalkName }.forEach {
            if (it in endpoint.referencedParametersWhenOptionalFalse(setOf(it))) {
                messageCollector.reportError(
                    "ServerDefault parameter \"$it\" must be wrapped in an optional block in the endpoint template, " +
                            "it can not appear in the endpoint when not specified.", declaration
                )
            }
        }

        val allowedOptionals =
            (nullableOptionalParameters.map { it.krosstalkName } + serverDefaultParameters.map { it.krosstalkName }).toSet()
        endpoint.usedOptionals().forEach {
            if (it !in allowedOptionals) {
                messageCollector.reportError(
                    "Used parameter \"$it\" as an optional in the endpoint template, but parameter is not" +
                            " an optional parameter (i.e. having @Optional) or a ServerDefault.", declaration
                )
            }
        }

        if (requireEmptyBody) {
            val topLevelParams = endpoint.topLevelParameters()
            buildSet {
                nonScopeValueParameters.filterNot { it.isSpecialParameter }.forEach { add(it.krosstalkName) }

                if (declaration.extensionReceiverParameter != null && !declaration.extensionReceiverParameter!!.param.isSpecialParameter)
                    add(extensionReceiver)

                if (declaration.dispatchReceiverParameter != null && !declaration.dispatchReceiverParameter!!.param.isSpecialParameter)
                    add(instanceReceiver)
            }.forEach {
                if (it in allowedOptionals) {
                    if (!endpoint.hasWhenNotNull(it)) {
                        messageCollector.reportError(
                            "Required an empty body with @EmptyBody, but optional parameter \"$it\" is not guaranteed to be in the endpoint when it is non-null.",
                            declaration
                        )
                    }
                } else {
                    if (it !in topLevelParams) {
                        messageCollector.reportError(
                            "Required an empty body with @EmptyBody, but non-optional parameter \"$it\" is not guaranteed to be in the endpoint.  " +
                                    "Make it a parameter that is not wrapped in an optional block.",
                            declaration
                        )
                    }
                }
            }
        }

        annotations.KrosstalkEndpoint?.httpMethod?.let { method ->
            if (method.lowercase() == "get" && hasArgs && !requireEmptyBody) {
                messageCollector.reportError(
                    "Can't use HTTP GET method without either having no parameters (including receivers) or using @EmptyBody.",
                    declaration
                )
            }
        }

        if (annotations.ExplicitResult != null && !isKrosstalkResult) {
            messageCollector.reportError(
                "Must have a return type of KrosstalkResult<T> or WithHeaders<KrosstalkResult<T>> to use @ExplicitResult.",
                declaration
            )
        }

        if (annotations.ExplicitResult == null && isKrosstalkResult) {
            messageCollector.reportError(
                "Must use @ExplicitResult to use KrosstalkResult in return type.",
                declaration
            )
        }

        if (annotations.RespondWithHeaders != null && !isWithHeaders) {
            messageCollector.reportError(
                "Must have a return type of WithHeaders<T> or KrosstalkResult<WithHeaders<T>> to use @RespondWithHeaders.",
                declaration
            )
        }

        if (annotations.RespondWithHeaders == null && isWithHeaders) {
            messageCollector.reportError(
                "Must use @RespondWithHeaders to use WithHeaders in return type.",
                declaration
            )
        }

        if (krosstalkClass.isClient && placeholderCall == null) {
            messageCollector.reportError(
                "Krosstalk methods for client should have placeholder krosstalkCall() body.",
                declaration
            )
        }

        if (placeholderCall != null && !krosstalkClass.isClient) {
            messageCollector.reportError(
                "Krosstalk method has placeholder body, but is not on a client Krosstalk.  The method should be implemented.",
                declaration
            )
        }

        checked = true
    }

    fun transform() {
        addToKrosstalkClass()

        if (krosstalkClass.isClient) {
            addCallMethodBody()
        }
    }

    private fun IrBuilderWithScope.buildCallLambda(): IrSimpleFunction {

        fun IrSimpleFunction.addCallMethodParameters(): Pair<IrValueParameter, IrValueParameter> {
            val args = addValueParameter {
                name = Name.identifier("arguments")
                type = IrSimpleTypeImpl(
                    Kotlin.Collections.Map(),
                    false,
                    listOf(context.irBuiltIns.stringType as IrTypeBase, IrStarProjectionImpl),
                    emptyList()
                )
            }

            val scopes = addValueParameter {
                name = Name.identifier("scopes")
                type = Krosstalk.Server.Plugin.ImmutableWantedScopes.resolveTypeWith()
            }
            return args to scopes
        }

        if (!krosstalkClass.isServer) {
            return buildLambda(declaration.returnType, { isSuspend = true }) {
                withBuilder {
                    addCallMethodParameters()

                    body = irJsExprBody(run {
                        val exceptionConstructor = Krosstalk.CallFromClientSideException().constructors.single()
                        irThrow(
                            irCallConstructor(exceptionConstructor, emptyList())
                                .withValueArguments(declaration.name.asString().asConst())
                        )
                    })
                }
            }
        }

        fun IrBuilderWithScope.getValueOrError(
            methodName: String,
            map: IrExpression,
            type: IrType,
            key: String,
            default: IrBody?,
            nullError: String,
            typeError: String,
            keyType: IrType = context.irBuiltIns.stringType,
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

        return buildLambda(declaration.returnType, { isSuspend = true }) {
            withBuilder {
                val (args, scopes) = addCallMethodParameters()

                body = irBlockBody {


                    val arguments = mutableMapOf<IrValueParameterSymbol, IrValueSymbol>()

                    val extensionReceiver = declaration.extensionReceiverParameter?.param?.let { param ->
                        irTemporary(
                            if (param.isIgnored) {
                                irNull(param.rawType)
                            } else {
                                getValueOrError(
                                    declaration.name.asString(),
                                    irGet(args),
                                    param.rawType,
                                    param.krosstalkName,
                                    param.run { defaultBody(arguments) },
                                    "No extension receiver argument, but it was required",
                                    "Extension receiver argument was type \$type, but parameter is of type \$required"
                                )
                            }, "dispatchReceiver"
                        ).also {
                            arguments[param.expectDeclaration?.symbol ?: param.declaration.symbol] = it.symbol
                        }
                    }

                    val dispatchReceiver = declaration.dispatchReceiverParameter?.param?.let { param ->
                        irTemporary(
                            getValueOrError(
                                declaration.name.asString(),
                                irGet(args),
                                param.rawType,
                                instanceReceiver,
                                null,
                                "No instance receiver argument, but it was required",
                                "Instance receiver argument was type \$type, but parameter is of type \$required"
                            ), "dispatchReceiver"
                        ).also {
                            arguments[param.expectDeclaration?.symbol ?: param.declaration.symbol] = it.symbol
                        }
                    }

                    val valueParameters = nonScopeValueParameters.associate { param ->
                        val value = if (param.isIgnored) {
                            if (param.defaultValue == null)
                                irNull(param.rawType)
                            else
                                param.substitutedDefaultValue(parent, arguments)
                        } else {
                            getValueOrError(
                                declaration.name.asString(),
                                irGet(args),
                                param.rawType,
                                param.krosstalkName,
                                param.run { defaultBody(arguments) },
                                "No argument for ${param.krosstalkName}, but it was required",
                                "Argument for ${param.krosstalkName} was type \$type, but the parameter is of type \$required"
                            )

                        }

                        val variable = irTemporary(value, param.realName)
                        arguments[param.expectDeclaration?.symbol ?: param.declaration.symbol] = variable.symbol

                        param.index to variable
                    }

                    +irReturn(irCall(declaration.symbol).apply {

                        this.extensionReceiver = extensionReceiver?.let { irGet(it) }
                        this.dispatchReceiver = dispatchReceiver?.let { irGet(it) }
                        valueParameters.forEach { (idx, value) ->
                            putValueArgument(idx, irGet(value))
                        }

                        requiredScopes.forEach { (param, cls) ->
                            val dataType =
                                cls.defaultType.raiseTo(Krosstalk.Server.Plugin.ServerScope()).typeArgument(0)
                            putValueArgument(
                                param.index,
                                irCall(Krosstalk.Server.Plugin.ImmutableWantedScopes.getRequiredInstance)
                                    .withDispatchReceiver(irGet(scopes))
                                    .withTypeArguments(cls.defaultType, dataType)
                                    .withValueArguments(irGetObject(cls.symbol), calculateName().asConst())
                            )
                        }

                        optionalScopes.forEach { (param, cls) ->
                            val dataType =
                                cls.defaultType.raiseTo(Krosstalk.Server.Plugin.ServerScope()).typeArgument(0)

                            putValueArgument(
                                param.index,
                                irCall(Krosstalk.Server.Plugin.ImmutableWantedScopes.getOptionalInstance)
                                    .withDispatchReceiver(irGet(scopes))
                                    .withTypeArguments(cls.defaultType, dataType)
                                    .withValueArguments(irGetObject(cls.symbol))
                            )
                        }

                    })
                }
            }
        }
    }

    private fun calculateName(): String {
        with(methodTransformer) {
            val paramHash = expectDeclaration?.paramHash() ?: declaration.paramHash()
            val name = if (annotations.KrosstalkMethod?.noParamHash == true) {
                if (declaration.name.asString() in seenNames.getOrDefault(
                        krosstalkClass.declaration.kotlinFqName,
                        mutableSetOf()
                    )
                ) {
                    messageCollector.reportError(
                        "Multiple methods for krosstalk object ${krosstalkClass.declaration.kotlinFqName} with name " +
                                "${declaration.name.asString()}.  All but one must use `noParamHash = false` in their @KrosstalkMethod annotations.",
                        declaration
                    )
                }
                declaration.name.asString()
            } else {
                declaration.name.asString() + "_$paramHash"
            }
            seenNames.getOrPut(krosstalkClass.declaration.kotlinFqName) { mutableSetOf() } += name
            return name
        }
    }

    private fun addToKrosstalkClass() {
        if (krosstalkClass.declaration.isExpect) return

        krosstalkClass.declaration.addAnonymousInitializer {
            parent = krosstalkClass.declaration
            body = withBuilder {
                irBlockBody {
                    +irCall(Krosstalk.Krosstalk.addMethod).apply {
                        dispatchReceiver = krosstalkClass.declaration.thisReceiver?.let { irGet(it) }
                        putTypeArgument(0, declaration.returnType)

                        var valueArguments = 0
                        fun addValueArgument(argument: IrExpression) = putValueArgument(valueArguments++, argument)

                        // Name
                        addValueArgument(calculateName().asConst())

                        // endpoint

                        addValueArgument(endpointTemplate.asConst())

                        // method
                        addValueArgument(
                            (this@KrosstalkFunction.annotations.KrosstalkEndpoint?.httpMethod
                                ?: defaultEndpointHttpMethod).asConst()
                        )

                        // content type
                        addValueArgument(
                            (this@KrosstalkFunction.annotations.KrosstalkEndpoint?.contentType ?: "").asConst()
                        )

                        // MethodTypes
                        addValueArgument(
                            irCall(Krosstalk.Serialization.MethodTypes().owner.primaryConstructor!!).apply {
                                val parameterMap: MutableMap<IrExpression, IrExpression> =
                                    nonScopeValueParameters.filter { it !in objectParameters && !it.isSpecialParameter }
                                        .associate {
                                            it.krosstalkName.asConst() to stdlib.reflect.typeOf(it.dataType)
                                        }.toMutableMap()

                                declaration.extensionReceiverParameter?.param?.let {
                                    if (it !in objectParameters && !it.isIgnored) {
                                        parameterMap[it.krosstalkName.asConst()] =
                                            stdlib.reflect.typeOf(it.dataType)
                                    }
                                }

                                declaration.dispatchReceiverParameter?.param?.let {
                                    if (it !in objectParameters) {
                                        parameterMap[it.krosstalkName.asConst()] =
                                            stdlib.reflect.typeOf(it.dataType)
                                    }
                                }

                                putValueArgument(
                                    0,
                                    stdlib.collections.mapOf(
                                        context.irBuiltIns.stringType,
                                        Kotlin.Reflect.KType().typeWith(),
                                        parameterMap
                                    )
                                )

                                putValueArgument(
                                    1, if (returnObject != null) {
                                        irNull(irTypeOf<KType>())
                                    } else {
                                        stdlib.reflect.typeOf(returnDataType)
                                    }
                                )
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

                        // useExplicitResult
                        addValueArgument(isKrosstalkResult.asConst())

                        // includeStacktrace
                        addValueArgument(
                            (this@KrosstalkFunction.annotations.ExceptionHandling?.includeStacktrace == true).asConst()
                        )

                        // rethrowServerException
                        addValueArgument((this@KrosstalkFunction.annotations.ExceptionHandling?.propagateServerExceptions == true).asConst())

                        // optionalParameters
                        addValueArgument(
                            nullableOptionalParameters.map { it.krosstalkName.asConst() }
                                .let { stdlib.collections.setOf(context.irBuiltIns.stringType, it) }
                        )

                        // serverDefaultParameters
                        addValueArgument(
                            serverDefaultParameters.map { it.krosstalkName.asConst() }
                                .let { stdlib.collections.setOf(context.irBuiltIns.stringType, it) }
                        )

                        // object params
                        addValueArgument(
                            objectParameters.associate {
                                (it.krosstalkName.asConst() as IrExpression) to irGetObjectValue(
                                    context.irBuiltIns.anyNType,
                                    it.constantObject!!.symbol
                                )
                            }
                                .let { stdlib.collections.mapOf(context.irBuiltIns.stringType, context.irBuiltIns.anyNType, it) }
                        )

                        // object return
                        addValueArgument(returnObject?.let { irGetObject(it.symbol) } ?: irNull())

                        // outerWithHeaders
                        addValueArgument(isOuterWithHeaders.asConst())

                        // innerWithHeaders
                        addValueArgument(isInnerWithHeaders.asConst())

                        // requestHeadersParam
                        addValueArgument(requestHeadersParameter?.krosstalkName?.asConst() ?: irNull())

                        // serverUrlParam
                        addValueArgument(serverUrlParameter?.krosstalkName?.asConst() ?: irNull())

                        // call function
                        // signature is (Map<String, *>, ImmutableWantedScopes)
                        val lambda = buildCallLambda().patchDeclarationParents(krosstalkClass.declaration)

                        addValueArgument(lambdaArgument(lambda))

                    }
                }
            }
        }
    }

    private fun addCallMethodBody() {
        if (!krosstalkClass.isClient)
            return

        declaration.withBuilder {

            serverDefaultParameters.forEach {
                it.declaration.defaultValue = irExprBody(irCall(Krosstalk.noneServerDefault))
            }

            declaration.body = irJsExprBody(irCall(Krosstalk.Client.call(), declaration.returnType).apply {

                extensionReceiver = irGetObject(krosstalkClass.declaration.symbol)
                putTypeArgument(0, declaration.returnType)
                putTypeArgument(1, krosstalkClass.declaration.defaultType.makeNotNull())

                val clientScopeType =
                    krosstalkClass.declaration.defaultType.raiseTo(Krosstalk.Client.KrosstalkClient())
                        .typeArgument(0)

                putTypeArgument(2, clientScopeType)

                val argumentsMap =
                    nonScopeValueParameters.filterNot { it.isIgnored }.associate { it.krosstalkName.asConst() to irGet(it.declaration) }
                        .toMutableMap<IrExpression, IrExpression>()

                declaration.extensionReceiverParameter?.param?.let {
                    if (!it.isIgnored)
                        argumentsMap[com.rnett.krosstalk.extensionReceiver.asConst()] = irGet(it.declaration)
                }
                declaration.dispatchReceiverParameter?.let {
                    argumentsMap[instanceReceiver.asConst()] = irGet(it)
                }

                val scopeList = mutableListOf<IrExpression>()

                requiredScopes.forEach { (param, cls) ->
                    scopeList.add(
                        irCall(Krosstalk.Client.Plugin.instanceToAppliedScope)
                            .withExtensionReceiver(irGet(param.declaration))
                            .withTypeArguments(cls.defaultType, context.irBuiltIns.anyType.makeNullable())
                    )
                }

                optionalScopes.forEach { (param, cls) ->
                    scopeList.add(
                        irCall(Krosstalk.Client.Plugin.instanceToAppliedScope)
                            .withExtensionReceiver(irGet(param.declaration))
                            .withTypeArguments(cls.defaultType, context.irBuiltIns.anyType.makeNullable())
                    )
                }

                putValueArgument(0, calculateName().asConst())
                putValueArgument(
                    1, stdlib.collections.mapOf(
                        context.irBuiltIns.stringType,
                        context.irBuiltIns.anyType.makeNullable(),
                        argumentsMap
                    )
                )
                putValueArgument(
                    2, stdlib.collections.listOfNotNull(
                        Krosstalk.Client.Plugin.AppliedClientScope()
                            .typeWithArguments(listOf(clientScopeType as IrTypeBase, IrStarProjectionImpl)),
                        scopeList
                    )
                )

                putValueArgument(3, placeholderCall!!.serverUrl)
                putValueArgument(4, placeholderCall!!.requestHeaders)
                putValueArgument(5, placeholderCall!!.scopes(this@withBuilder))
            })
        }
    }
}