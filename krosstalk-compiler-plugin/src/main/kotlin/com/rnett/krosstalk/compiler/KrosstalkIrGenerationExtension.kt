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
        //TODO phase to make sure there's no annotations on non-krosstalk methods, all `krosstalkCall()`s get replaced, etc
//        log("Post transform:", moduleFragment.dump(true))
//        Tester(pluginContext).lower(moduleFragment)
    }

}