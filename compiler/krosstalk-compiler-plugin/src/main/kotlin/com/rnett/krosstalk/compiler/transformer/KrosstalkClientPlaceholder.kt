package com.rnett.krosstalk.compiler.transformer

import com.rnett.krosstalk.compiler.Krosstalk
import com.rnett.plugin.ir.HasContext
import com.rnett.plugin.ir.valueArgumentsByName
import com.rnett.plugin.stdlib.Kotlin
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.starProjectedType
import org.jetbrains.kotlin.ir.util.kotlinFqName

fun IrCall.isClientPlaceholder() = this.symbol.owner.kotlinFqName == Krosstalk.Client.clientPlaceholderFqName

private const val SERVER_URL_NAME = "serverUrl"
private const val REQUEST_HEADERS_NAME = "requestHeaders"
private const val SCOPES_NAME = "scopes"

class KrosstalkClientPlaceholder(val call: IrCall, override val context: IrPluginContext) : HasContext {
    init {
        if (!call.isClientPlaceholder())
            error("Not a client placeholder call")
    }

    private val arguments by lazy { call.valueArgumentsByName() }

    val serverUrl: IrExpression get() = arguments[SERVER_URL_NAME] ?: nullConst(context.irBuiltIns.stringType.makeNullable())

    val requestHeaders: IrExpression get() = arguments[REQUEST_HEADERS_NAME] ?: nullConst(Krosstalk.Headers.resolveTypeWith().makeNullable())

    fun scopes(builder: IrBuilderWithScope): IrExpression {
        val expr = arguments[SCOPES_NAME] ?: nullConst(
            Kotlin.Collections.Iterable.resolveTypeWith(Krosstalk.ScopeInstance().starProjectedType).makeNullable()
        )
        if (expr is IrVararg) {
            return builder.stdlib.collections.listOfVararg(expr)
        }
        return expr
    }

}