package com.rnett.krosstalk.compiler

import com.google.auto.service.AutoService
import com.rnett.plugin.ir.messageCollector
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import java.io.File

private val logFile by lazy {
    File("C:\\Users\\jimne\\Desktop\\My Stuff\\krosstalk\\log").also { it.writeText("") }
}

fun log(it: Any?) = logFile.appendText(it.toString() + "\n")
fun log(key: String, it: Any?) = log("$key: $it")

@AutoService(ComponentRegistrar::class)
class KrosstalkComponentRegistar : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        IrGenerationExtension.registerExtension(project, KrosstalkIrGenerationExtension(configuration.messageCollector))
    }

}