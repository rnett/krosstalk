package com.rnett.krosstalk.compiler

import com.rnett.krosstalk.compiler.transformer.KrosstalkMethodTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.platform.konan.isNative


class KrosstalkIrGenerationExtension(val messageCollector: MessageCollector) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        //TODO workaround for https://youtrack.jetbrains.com/issue/KT-46896
//        if(moduleFragment.descriptor.platform.isNative())
//            pluginContext.irBuiltIns.suspendFunction(2)

        if (pluginContext.referenceClass(Krosstalk.Krosstalk.fqName) != null) {
            KrosstalkMethodTransformer(pluginContext, messageCollector).lower(moduleFragment)
        }
        //TODO phase to make sure there's no annotations on non-krosstalk methods, all `krosstalkCall()`s get replaced, etc
    }

}