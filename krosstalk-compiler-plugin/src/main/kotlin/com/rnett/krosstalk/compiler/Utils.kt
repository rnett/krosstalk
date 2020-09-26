package com.rnett.krosstalk.compiler

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.js.JsPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatform
import org.jetbrains.kotlin.platform.konan.NativePlatform

enum class Platform {
    JVM, JS, Common, Native, Unknown;

    companion object {
        fun detectPlatform(moduleDescriptor: ModuleDescriptor): Platform {
            val platforms = (moduleDescriptor.platform ?: return Unknown).toList()
            return when {
                platforms.all { it is JvmPlatform } -> JVM
                platforms.all { it is JsPlatform } -> JS
                platforms.all { it is NativePlatform } -> Native
                else -> Common
            }

        }

        fun init(moduleDescriptor: IrModuleFragment) {
            _platform = detectPlatform(moduleDescriptor.descriptor)
        }

        private var _platform: Platform? = null

        val hasInited get() = _platform != null
        val platform get() = _platform ?: error("init() has not been called yet")
    }
}

fun Iterable<IrConstructorCall>.getAnnotation(name: FqName): IrConstructorCall? = firstOrNull { it.symbol.owner.parentAsClass.fqNameWhenAvailable == name }
fun Iterable<IrConstructorCall>.getAnnotations(name: FqName): List<IrConstructorCall> = filter { it.symbol.owner.parentAsClass.fqNameWhenAvailable == name }
fun Iterable<IrConstructorCall>.hasAnnotation(name: FqName) = any { it.symbol.owner.parentAsClass.fqNameWhenAvailable == name }
