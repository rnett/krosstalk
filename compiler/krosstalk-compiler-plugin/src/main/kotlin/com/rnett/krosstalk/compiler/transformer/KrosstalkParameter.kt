package com.rnett.krosstalk.compiler.transformer

import com.rnett.krosstalk.compiler.Krosstalk
import com.rnett.krosstalk.compiler.KrosstalkAnnotations
import com.rnett.krosstalk.extensionReceiver
import com.rnett.krosstalk.instanceReceiver
import com.rnett.plugin.ir.HasContext
import com.rnett.plugin.ir.KnowsCurrentFile
import com.rnett.plugin.ir.typeArgument
import com.rnett.plugin.naming.isClassifierOf
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.parent
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.hasDefaultValue
import org.jetbrains.kotlin.ir.util.isAnonymousObject
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.parentClassOrNull

class KrosstalkParameter(
    private val krosstalkFunction: KrosstalkFunction,
    val declaration: IrValueParameter,
    val expectDeclaration: IrValueParameter? = with(krosstalkFunction) { declaration.expect() },
) : HasContext by krosstalkFunction, KnowsCurrentFile by krosstalkFunction {
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

    val isExtensionReceiver get() = krosstalkFunction.declaration.extensionReceiverParameter == declaration
    val isDispatchReceiver get() = krosstalkFunction.declaration.dispatchReceiverParameter == declaration
    val isValueParameter by lazy { declaration in krosstalkFunction.declaration.valueParameters }

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

        with(krosstalkFunction) {
            dataType.expectableObject()
        }
    }

    private fun reportError(error: String) {
        krosstalkFunction.messageCollector.reportError("Error on parameter \"$realName\": $error", declaration)
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
            if (scopeClass.parentClassOrNull?.kotlinFqName != krosstalkFunction.krosstalkClass.declaration.kotlinFqName &&
                scopeClass.parentClassOrNull?.kotlinFqName != krosstalkFunction.actualKrosstalkClass.kotlinFqName
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
                krosstalkFunction.messageCollector.reportWarning(
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

        if (isRequestHeaders && !dataType.isClassifierOf(Krosstalk.Headers)) {
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