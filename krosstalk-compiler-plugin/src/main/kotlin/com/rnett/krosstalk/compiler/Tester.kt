package com.rnett.krosstalk.compiler

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrFile

class Tester(override val context: IrPluginContext) : HasContext, IrElementTransformerVoidWithContext(), FileLoweringPass {


    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid()
    }

}