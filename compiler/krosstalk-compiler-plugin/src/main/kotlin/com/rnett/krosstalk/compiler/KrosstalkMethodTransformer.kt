package com.rnett.krosstalk.compiler

import com.rnett.krosstalk.*
import com.rnett.krosstalk.endpoint.Endpoint
import com.rnett.plugin.ir.*
import com.rnett.plugin.naming.ClassRef
import com.rnett.plugin.naming.isClassifierOf
import com.rnett.plugin.stdlib.Kotlin
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.allParameters
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.types.impl.IrTypeBase
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.multiplatform.findExpects
import kotlin.collections.set
import kotlin.math.absoluteValue
import kotlin.reflect.KType

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

    inner class KrosstalkClass(val declaration: IrClass) {
        val scopes by lazy {
            declaration.declarations.filterIsInstance<IrClass>()
                .filter { it.isNonCompanionObject && it.isSubclassOf(Krosstalk.Scope) }
        }

        val isClient by lazy {
            Krosstalk.Client.KrosstalkClient.resolveOrNull() != null && declaration.isSubclassOf(
                Krosstalk.Client.KrosstalkClient
            )
        }
        val isServer by lazy {
            Krosstalk.Server.KrosstalkServer.resolveOrNull() != null && declaration.isSubclassOf(
                Krosstalk.Server.KrosstalkServer
            )
        }

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
                    val scopeType = declaration.defaultType.raiseTo(Krosstalk.Client.KrosstalkClient()).typeArgument(0)
                    if (!it.defaultType.isSubtypeOf(scopeType, context.irBuiltIns))
                        messageCollector.reportError(
                            "All scopes in a Krosstalk Client object must extend the client's scope type ${scopeType.render()}, ${it.name} does not.",
                            it
                        )
                }
                if (isServer) {
                    val scopeType = declaration.defaultType.raiseTo(Krosstalk.Server.KrosstalkServer()).typeArgument(0)
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

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    fun IrClass.expect(): IrClass? {
        if (!this.descriptor.isActual)
            return null
        else
            return context.symbolTable.referenceClass(
                this.descriptor.findExpects().single() as ClassDescriptor
            ).owner
    }

    inner class KrosstalkFunction(val declaration: IrSimpleFunction) {

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
                return@lazy KrosstalkClass(expectClass)
            }

            KrosstalkClass(actualKrosstalkClass)
        }

        val endpointTemplate by lazy { annotations.KrosstalkEndpoint?.endpoint ?: defaultEndpoint }

        @OptIn(InternalKrosstalkApi::class)
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

        private fun IrValueParameter.expect() = expectParameters?.getValue(name)

        private fun IrType.expectableObject(): IrClass? {
            if (isNullable())
                return null

            val cls = classOrNull?.owner?.let { it.expect() ?: it } ?: return null
            return if ((cls.isObject || cls.isCompanion) && !cls.isAnonymousObject)
                classOrNull?.owner
            else
                null
        }

        inner class KrosstalkParameter(
            val declaration: IrValueParameter,
            val expectDeclaration: IrValueParameter? = declaration.expect()
        ) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as KrosstalkParameter

                if (declaration != other.declaration) return false

                return true
            }

            override fun hashCode(): Int {
                return declaration.hashCode()
            }

            inline val index: Int get() = declaration.index
            inline val rawType: IrType get() = declaration.type

            val annotations by lazy { KrosstalkAnnotations(expectDeclaration?.annotations.orEmpty() + declaration.annotations) }

            val hasDefault by lazy { declaration.hasDefaultValue() || expectDeclaration?.hasDefaultValue() == true }
            val defaultValue by lazy { declaration.defaultValue ?: expectDeclaration?.defaultValue }

            val isExtensionReceiver get() = this@KrosstalkFunction.declaration.extensionReceiverParameter == declaration
            val isDispatchReceiver get() = this@KrosstalkFunction.declaration.dispatchReceiverParameter == declaration
            val isValueParameter by lazy { declaration in this@KrosstalkFunction.declaration.valueParameters }

            val isScope by lazy { declaration.type.isClassifierOf(Krosstalk.ScopeInstance) }
            val isOptionalScope get() = declaration.type.isNullable() && isScope
            val scopeClass by lazy { declaration.type.typeArgument(0).classOrNull!!.owner }

            val isOptionalParam by lazy { annotations.Optional != null }
            val isServerDefault by lazy { declaration.type.isClassifierOf(Krosstalk.ServerDefault) }

            val isRequestHeaders by lazy { annotations.RequestHeaders != null }
            val isServerURL by lazy { annotations.ServerURL != null }

            /**
             * Is this a parameter that will be specially handled in call/handle methods?
             *
             * If so, it will still be extracted from the argument map server-side (in the call lambda),
             * and added to the argument map client side, but will not be registered in MethodTypes and thus
             * should not be passed via serialization.
             */
            val isSpecialParameter by lazy { isRequestHeaders || isServerURL }

            val realName get() = declaration.name.asString()
            val krosstalkName: String by lazy {
                when {
                    isExtensionReceiver -> extensionReceiver
                    isDispatchReceiver -> instanceReceiver
                    else -> declaration.name.asString()
                }
            }

            val dataType by lazy {
                var type = rawType
                if (type.isClassifierOf(Krosstalk.ServerDefault))
                    type = type.typeArgument(0)

                type
            }

            val constantObject: IrClass? by lazy {
                if (isServerDefault) return@lazy null

                dataType.expectableObject()
            }

            private fun reportError(error: String) {
                messageCollector.reportError("Error on parameter \"$realName\": $error", declaration)
            }

            private var checked = false
            fun check() {
                if (checked)
                    return

                if (isScope && isOptionalParam) {
                    reportError("Can't use @Optional on scopes, to make a scope optional just make it nullable")
                }

                if (isServerDefault && !hasDefault) {
                    reportError("Must specify a default value for ServerDefault parameters.")
                }

                if (isServerDefault && !isValueParameter) {
                    reportError("Can only use ServerDefault as a value parameter.")
                }

                if (isServerDefault && isOptionalParam) {
                    reportError("Can't use @Optional on ServerDefault parameter.")
                }

                if (isServerURL && isRequestHeaders) {
                    reportError("Can't have a parameter be both @ServerURL and @RequestHeaders")
                }

                if (isOptionalParam && isServerURL) {
                    reportError("@ServerURL parameters can't be @Optional")
                }

                if (isOptionalParam && isRequestHeaders) {
                    reportError("@RequestHeaders parameters can't be @Optional")
                }

                if (expectDeclaration != null) {
                    declaration.annotations.forEach {
                        val annotationClass = it.symbol.owner.constructedClass
                        if (annotationClass.hasAnnotation(Krosstalk.Annotations.TopLevelOnly())) {
                            reportError(
                                "Krosstalk annotation ${annotationClass.name} must only be specified at the top level expect function."
                            )
                        }
                    }
                }

                if (isScope) {
                    if (scopeClass.isCompanion || scopeClass.isAnonymousObject || !scopeClass.isObject) {
                        reportError(
                            "Scope parameter has invalid scope type.  Scopes must be objects nested in the Krosstalk class"
                        )
                    }

                    //TODO checking symbols or signatures here sometimes gives false positive !=s
                    if (scopeClass.parentClassOrNull?.kotlinFqName != krosstalkClass.declaration.kotlinFqName &&
                        scopeClass.parentClassOrNull?.kotlinFqName != actualKrosstalkClass.kotlinFqName
                    ) {
                        reportError(
                            "Scope parameter has invalid scope type.  Scopes must be objects nested in the Krosstalk class"
                        )
                    }
                }

                if (rawType.isSubclassOf(Krosstalk.ScopeInstance) && !rawType.isClassifierOf(Krosstalk.ScopeInstance)) {
                    reportError(
                        "Krosstalk method scope parameters should only be ScopeInstance or ScopeInstance?, not a subclass"
                    )
                }

                if (rawType.isClassifierOf(Krosstalk.ScopeInstance)) {
                    if (rawType.isNullable() && !hasDefault) {
                        messageCollector.reportWarning(
                            "Optional scope \"$realName\" with no default value, should probably add a null default.",
                            declaration
                        )
                    }
                }

                if (isOptionalParam && !dataType.isNullable()) {
                    reportError("@Optional parameters must be nullable.")
                }

                if (isServerURL && !(dataType == context.irBuiltIns.stringType || dataType == context.irBuiltIns.stringType.makeNullable())) {
                    reportError("Type of @ServerURL parameter must be String or String?")
                }

                if (isRequestHeaders && dataType != Headers) {
                    reportError("Type of @RequestHeaders parameter must be Headers (com.rnett.krosstalk.Headers)")
                }

                checked = true
            }

            fun IrBuilderWithScope.defaultBody(): IrBody? =
                when {
                    isServerDefault -> defaultValue!!.deepCopyWithSymbols(this.parent)
                    isOptionalParam -> irExprBody(irNull(rawType))
                    else -> null
                }
        }

        val parameters by lazy { declaration.allParameters.map { KrosstalkParameter(it) } }
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

        val optionalParameters by lazy {
            parameters.filter { it.isOptionalParam }
        }

        val serverDefaultParameters by lazy {
            parameters.filter { it.isServerDefault }
        }

        val allNonScopeParameters by lazy {
            nonScopeValueParameters + listOfNotNull(
                declaration.extensionReceiverParameter?.param,
                declaration.dispatchReceiverParameter?.param
            ).filterNot { it.isScope }
        }

        @OptIn(InternalKrosstalkApi::class)
        val objectParameters by lazy {
            if (annotations.PassObjects != null)
                emptySet()
            else
                parameters.filter { it.constantObject != null && it.krosstalkName !in endpoint.allReferencedParameters() }
                    .toSet()
        }

        private fun IrType.unwrapClassifier(klass: ClassRef) = if (this.isClassifierOf(klass)) typeArgument(0) else this

        val returnDataType by lazy {
            val type = declaration.returnType

            if (type.isClassifierOf(Krosstalk.KrosstalkResult)) {
                type.typeArgument(0).unwrapClassifier(Krosstalk.WithHeaders)
            } else if (type.isClassifierOf(Krosstalk.WithHeaders)) {
                type.typeArgument(0).unwrapClassifier(Krosstalk.KrosstalkResult)
            } else
                type
        }

        val isWithHeaders by lazy {
            val type = declaration.returnType
            type.isClassifierOf(Krosstalk.WithHeaders) ||
                    (type.isClassifierOf(Krosstalk.KrosstalkResult) && type.typeArgument(0)
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
            type.isClassifierOf(Krosstalk.KrosstalkResult) ||
                    (type.isClassifierOf(Krosstalk.WithHeaders) && type.typeArgument(0)
                        .isClassifierOf(Krosstalk.KrosstalkResult))
        }

        val returnObject by lazy {
            if (annotations.PassObjects?.returnToo == true)
                return@lazy null
            returnDataType.expectableObject()
        }

        @OptIn(ExperimentalStdlibApi::class, com.rnett.krosstalk.InternalKrosstalkApi::class)
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
            if (Krosstalk.KrosstalkResult() in allReturnTypeArgs) {
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
                nonScopeValueParameters.filterNot { it.isSpecialParameter }.map { it.krosstalkName }.toSet()
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
                (optionalParameters.map { it.krosstalkName } + serverDefaultParameters.map { it.krosstalkName }).toSet()
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
                    addAll(paramNames)

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
                if (method.toLowerCase() == "get" && hasArgs && !requireEmptyBody) {
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

            val isPlaceholderBody = when (val body = declaration.body) {
                is IrExpressionBody -> {
                    body.expression.let { it is IrCall && it.symbol.owner.kotlinFqName == Krosstalk.Client.clientPlaceholder.fqName }
                }
                is IrBlockBody -> {
                    body.statements.size == 1 && body.statements.single()
                        .let { it is IrCall && it.symbol.owner.kotlinFqName == Krosstalk.Client.clientPlaceholder.fqName }
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

        fun transform() {
            addToKrosstalkClass()

            if (krosstalkClass.isClient) {
                addCallMethodBody()
            }
        }

        fun IrBuilderWithScope.buildCallLambda(): IrSimpleFunction {
            if (!krosstalkClass.isServer) {
                return buildLambda(declaration.returnType, { isSuspend = true }) {
                    withBuilder {
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
                        type = Krosstalk.Server.Plugin.ImmutableWantedScopes.resolveTypeWith()
                    }

                    body = irJsExprBody(irCall(declaration.symbol).apply {
                        nonScopeValueParameters.forEach { param ->
                            putValueArgument(
                                param.index,
                                getValueOrError(
                                    declaration.name.asString(),
                                    irGet(args),
                                    param.rawType,
                                    param.krosstalkName,
                                    param.run { defaultBody() },
                                    "No argument for ${param.krosstalkName}, but it was required",
                                    "Argument for ${param.krosstalkName} was type \$type, but the parameter is of type \$required"
                                )
                            )
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

                        declaration.extensionReceiverParameter?.param?.let { param ->
                            extensionReceiver = getValueOrError(
                                declaration.name.asString(),
                                irGet(args),
                                param.rawType,
                                param.krosstalkName,
                                param.run { defaultBody() },
                                "No extension receiver argument, but it was required",
                                "Extension receiver argument was type \$type, but parameter is of type \$required"
                            )
                        }

                        declaration.dispatchReceiverParameter?.let {
                            dispatchReceiver = getValueOrError(
                                declaration.name.asString(),
                                irGet(args),
                                it.type,
                                instanceReceiver,
                                null,
                                "No instance receiver argument, but it was required",
                                "Instance receiver argument was type \$type, but parameter is of type \$required"
                            )
                        }

                    })
                }
            }
        }

        private fun calculateName(): String {
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

        fun addToKrosstalkClass() {
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
                                        if (it !in objectParameters) {
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
                                            stringType,
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
                                (this@KrosstalkFunction.annotations.ExplicitResult?.includeStacktrace == true).asConst()
                            )

                            // rethrowServerException
                            addValueArgument((this@KrosstalkFunction.annotations.ExplicitResult?.propagateServerExceptions == true).asConst())

                            // optionalParameters
                            addValueArgument(
                                optionalParameters.map { it.krosstalkName.asConst() }
                                    .let { stdlib.collections.setOf(stringType, it) }
                            )

                            // serverDefaultParameters
                            addValueArgument(
                                serverDefaultParameters.map { it.krosstalkName.asConst() }
                                    .let { stdlib.collections.setOf(stringType, it) }
                            )

                            // object params
                            addValueArgument(
                                objectParameters.associate {
                                    (it.krosstalkName.asConst() as IrExpression) to irGetObjectValue(
                                        context.irBuiltIns.anyNType,
                                        it.constantObject!!.symbol
                                    )
                                }
                                    .let { stdlib.collections.mapOf(stringType, context.irBuiltIns.anyNType, it) }
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

        fun addCallMethodBody() {
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
                        nonScopeValueParameters.associate { it.krosstalkName.asConst() to irGet(it.declaration) }
                            .toMutableMap<IrExpression, IrExpression>()

                    declaration.extensionReceiverParameter?.let {
                        argumentsMap[com.rnett.krosstalk.extensionReceiver.asConst()] = irGet(it)
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
                            stringType,
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
                })
            }

        }
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {

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