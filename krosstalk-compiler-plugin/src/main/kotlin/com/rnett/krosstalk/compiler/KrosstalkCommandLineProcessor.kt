package com.rnett.krosstalk.compiler

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor

@AutoService(CommandLineProcessor::class)
class KrosstalkCommandLineProcessor : CommandLineProcessor {
    override val pluginId: String = "com.rnett.krosstalk-compiler-plugin"
    override val pluginOptions: Collection<AbstractCliOption> = emptyList()
}