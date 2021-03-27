package com.rnett.krosstalk

import kotlin.reflect.KClass

internal actual fun getClassName(klass: KClass<*>): String? {
    return klass.qualifiedName
}