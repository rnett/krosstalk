package kbuild

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
object OS {
    val hostOs: String get() = System.getProperty("os.name")
    val isWindows get() = hostOs.startsWith("Windows")
    val isMacOs get() = hostOs == "Mac OS X"

    val isLinux get() = hostOs == "Linux"

    fun ifWindows(block: () -> Unit) {
        contract {
            callsInPlace(block, InvocationKind.AT_MOST_ONCE)
        }
        if (isWindows)
            block()
    }

    fun ifMac(block: () -> Unit) {
        contract {
            callsInPlace(block, InvocationKind.AT_MOST_ONCE)
        }
        if (isMacOs)
            block()
    }

    fun ifLinux(block: () -> Unit) {
        contract {
            callsInPlace(block, InvocationKind.AT_MOST_ONCE)
        }
        if (isLinux)
            block()
    }
}