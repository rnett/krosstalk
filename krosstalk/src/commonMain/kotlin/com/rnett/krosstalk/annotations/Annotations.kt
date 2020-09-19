package com.rnett.krosstalk.annotations

import com.rnett.krosstalk.Krosstalk
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
annotation class KrosstalkHost

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
annotation class KrosstalkMethod(val klass: KClass<out Krosstalk>, vararg val scopes: String)