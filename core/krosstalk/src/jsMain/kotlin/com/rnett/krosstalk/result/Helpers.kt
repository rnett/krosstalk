package com.rnett.krosstalk.result

import kotlin.reflect.KClass

//TODO use qualified name
internal actual fun getClassName(klass: KClass<*>): String? {
    return klass.simpleName
}