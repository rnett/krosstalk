package com.rnett.krosstalk.result

import kotlin.reflect.KClass

internal expect fun getClassName(klass: KClass<*>): String?