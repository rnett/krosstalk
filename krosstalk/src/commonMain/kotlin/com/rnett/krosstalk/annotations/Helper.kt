package com.rnett.krosstalk.annotations

//
//fun Map<AnnotationCompanion<*>, Map<String, Any?>>.toAnnotationMap(): AnnotationMap = mapValues { ResolvedAnnotation(it.key, it.value) }
//
//typealias AnnotationMap = Map<AnnotationCompanion<*>, ResolvedAnnotation<*>>
//
//operator fun <T, A: Annotation> AnnotationMap.get(field: AnnotationCompanion<A>.Field<T>): T? = this[field.annotation]?.let {
//    (it as ResolvedAnnotation<A>)[field]
//}
//
//data class ResolvedAnnotation<A: Annotation>(val annotationCompanion: AnnotationCompanion<A>, val fields: Map<String, Any?>){
//    operator fun <T> get(field: AnnotationCompanion<A>.Field<T>): T {
//        check(field.annotation == annotationCompanion){ "Can't get field for different annotation" }
//        return fields[field.name] as T ?: field.default ?: error("No value for required field ${field.name}")
//    }
//
//    operator fun <T> AnnotationCompanion<A>.Field<T>.unaryPlus() = this@ResolvedAnnotation[this]
//    fun <T> AnnotationCompanion<A>.Field<T>.get() = this@ResolvedAnnotation[this]
//
//}
//
//abstract class AnnotationCompanion<A: Annotation>(){
//    inner class FieldDelegate<T>(val default: T?): ReadOnlyProperty<Any?, Field<T>> {
//        override fun getValue(thisRef: Any?, property: KProperty<*>): Field<T> {
//            return Field(property.name, default)
//        }
//    }
//
//    protected fun <T> field(default: (() -> T)? = null) = FieldDelegate(default)
//
//    protected operator fun <T> KProperty1<A, T>.getValue(thisRef: Any?, property: KProperty<*>): Field<T>{
//        return Field(name, null)
//    }
//
//    protected infix fun <T> KProperty1<A, T>.withDefault(default: T) = FieldDelegate(default)
//
//    inner class Field<T>(val name: String, val default: T?){
//        val annotation = this@AnnotationCompanion
//    }
//}