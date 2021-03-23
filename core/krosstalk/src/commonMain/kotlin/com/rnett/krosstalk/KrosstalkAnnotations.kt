package com.rnett.krosstalk

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

//TODO unnecessary, just do it in the compiler

// fields should include defaults
internal class AnnotationSpec<T : Annotation>(
    val klass: KClass<T>,
    private val fields: Map<String, Any?>,
    val metaAnnotations: KrosstalkAnnotations = KrosstalkAnnotations(emptyMap()),
) {
    operator fun <R> get(prop: KProperty1<T, R>) = fields[prop.name] as R

    inline fun <reified T : Annotation> hasMetaAnnotation() = T::class in metaAnnotations
}

internal class KrosstalkAnnotations(val annotations: Map<KClass<out Annotation>, AnnotationSpec<*>>) {
    operator fun <T : Annotation> get(klass: KClass<T>): AnnotationSpec<T>? =
        annotations[klass]?.let { it as AnnotationSpec<T> }

    inline fun <reified T : Annotation> annotation(): AnnotationSpec<T>? = get(T::class)
    inline operator fun <reified T : Annotation, R> get(prop: KProperty1<T, R>): R? = annotation<T>()?.get(prop)

    operator fun contains(klass: KClass<out Annotation>) = klass in annotations
}