package com.rnett.krosstalk.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

class KrosstalkIrGenerationExtension(val messageCollector: MessageCollector) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        Platform.init(moduleFragment)
        KrosstalkMethodTransformer(pluginContext, messageCollector).lower(moduleFragment)
//        log("Post transform:", moduleFragment.dump(true))
//        Tester(pluginContext).lower(moduleFragment)
    }

}