package com.rnett.krosstalk.compiler.transformer

import com.rnett.krosstalk.compiler.Krosstalk
import com.rnett.plugin.ir.HasContext
import com.rnett.plugin.ir.KnowsCurrentFile
import com.rnett.plugin.ir.addAnonymousInitializer
import com.rnett.plugin.ir.raiseTo
import com.rnett.plugin.ir.typeArgument
import com.rnett.plugin.ir.withDispatchReceiver
import com.rnett.plugin.ir.withValueArguments
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.isInCurrentModule
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.types.isSubtypeOf
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.isAnonymousObject
import org.jetbrains.kotlin.ir.util.isNonCompanionObject
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.ir.util.render

class KrosstalkClass(val declaration: IrClass, private val methodTransformer: KrosstalkMethodTransformer) :
    HasContext by methodTransformer, KnowsCurrentFile by methodTransformer {

    val messageCollector: MessageCollector = methodTransformer.messageCollector

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