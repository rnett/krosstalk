package com.rnett.krosstalk.compiler

import com.rnett.krosstalk.defaultEndpoint
import com.rnett.krosstalk.defaultEndpointHttpMethod
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.FqName
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

fun IrAnnotationContainer.hasKrosstalkSubAnnotation() = annotations.any {
    it.isKrosstalkAnnotation()
}

fun IrConstructorCall.isKrosstalkAnnotation() =
    symbol.owner.constructedClass.fqNameForIrSerialization in KrosstalkAnnotation.knownAnnotations

fun IrAnnotationContainer.krosstalkAnnotations() = KrosstalkAnnotations(annotations)

data class KrosstalkAnnotations(val annotations: Set<KrosstalkAnnotation>) :
    Set<KrosstalkAnnotation> by annotations {
    constructor(annotations: List<IrConstructorCall>) : this(annotations.filter { it.isKrosstalkAnnotation() }
        .map { KrosstalkAnnotation(it) }.toSet())

    @Suppress("UNCHECKED_CAST")
    operator fun <A : KrosstalkAnnotation> get(klass: KClass<A>): A? =
        annotations.singleOrNull { it::class == klass } as A?

    inline fun <reified A : KrosstalkAnnotation> get(): A? = get(A::class)

    fun <A : KrosstalkAnnotation> repeatable(klass: KClass<A>): List<A> =
        annotations.filter { it::class == klass }.map {
            @Suppress("UNCHECKED_CAST")
            it as A
        }

    inline fun <reified A : KrosstalkAnnotation> repeatable() = repeatable(A::class)

    inner class AnnotationDelegate<A : KrosstalkAnnotation>(val klass: KClass<A>) :
        ReadOnlyProperty<Any?, A?> {
        val annotation = get(klass)
        override fun getValue(thisRef: Any?, property: KProperty<*>): A? = annotation
    }

    private inline fun <reified A : KrosstalkAnnotation> annotation() = AnnotationDelegate(A::class)

    inner class RepeatableAnnotationDelegate<A : KrosstalkAnnotation>(val klass: KClass<A>) :
        ReadOnlyProperty<Any?, List<A>> {
        val annotations = repeatable(klass)
        override fun getValue(thisRef: Any?, property: KProperty<*>): List<A> = annotations
    }

    private inline fun <reified A : KrosstalkAnnotation> repeatableAnnotation() = RepeatableAnnotationDelegate(A::class)

    val KrosstalkMethod by annotation<KrosstalkAnnotation.KrosstalkMethod>()

    val KrosstalkEndpoint by annotation<KrosstalkAnnotation.KrosstalkEndpoint>()

    val EmptyBody by annotation<KrosstalkAnnotation.EmptyBody>()
    val ExplicitResult by annotation<KrosstalkAnnotation.ExplicitResult>()
    val ExceptionHandling by annotation<KrosstalkAnnotation.ExceptionHandling>()

    val Optional by annotation<KrosstalkAnnotation.Optional>()

    val PassObjects by annotation<KrosstalkAnnotation.PassObjects>()

    val RespondWithHeaders by annotation<KrosstalkAnnotation.RespondWithHeaders>()


    val ServerURL by annotation<KrosstalkAnnotation.ServerURL>()
    val RequestHeaders by annotation<KrosstalkAnnotation.RequestHeaders>()
    val Ignore by annotation<KrosstalkAnnotation.Ignore>()
}

data class WrapperDelegate<T>(val value: T) {
    inline operator fun getValue(thisRef: Any?, property: KProperty<*>) = value
}

sealed class KrosstalkAnnotation(val call: IrConstructorCall, name: String) {
    val annotationName = FqName("$annotationPackage.$name")
    val arguments = call.getArgumentsWithIr().toMap().mapKeys { it.key.name.asString() }
    val clientOnly = call.symbol.owner.constructedClass.hasAnnotation(Krosstalk.Annotations.ClientOnly.fqName)
    val topLevelOnly = call.symbol.owner.constructedClass.hasAnnotation(Krosstalk.Annotations.TopLevelOnly.fqName)

    init {
        val constructedName = call.symbol.owner.constructedClass.fqNameForIrSerialization
        check(constructedName == annotationName) { "Wrong call for annotation.  Expected $annotationName, got $constructedName" }
    }

    override fun toString(): String = annotationName.toString()

    inner class ClassFieldDelegate :
        PropertyDelegateProvider<Any?, WrapperDelegate<IrClassReference>> {
        override fun provideDelegate(thisRef: Any?, property: KProperty<*>): WrapperDelegate<IrClassReference> {
            val name = property.name
            val expr = arguments[name]
                ?: error("Required field $name missing")

            return WrapperDelegate(expr as IrClassReference)
        }
    }

    inner class FieldDelegate<T>(val default: T?) :
        PropertyDelegateProvider<Any?, WrapperDelegate<T>> {
        override fun provideDelegate(thisRef: Any?, property: KProperty<*>): WrapperDelegate<T> {
            val name = property.name
            val expr = arguments[name] ?: return default?.let { WrapperDelegate(it) }
                ?: error("Required field $name missing")

            @Suppress("UNCHECKED_CAST")
            return WrapperDelegate((expr as IrConst<T>).value)
        }
    }

    inner class OptionalFieldDelegate<T> : PropertyDelegateProvider<Any?, WrapperDelegate<T?>> {
        override fun provideDelegate(thisRef: Any?, property: KProperty<*>): WrapperDelegate<T?> {
            val name = property.name
            val expr = arguments[name] ?: return WrapperDelegate(null)

            @Suppress("UNCHECKED_CAST")
            return WrapperDelegate((expr as IrConst<T>).value)
        }
    }

    inner class VarargFieldDelegate<T> : PropertyDelegateProvider<Any?, WrapperDelegate<List<T>>> {
        override fun provideDelegate(thisRef: Any?, property: KProperty<*>): WrapperDelegate<List<T>> {
            val name = property.name
            val expr = arguments[name] ?: return WrapperDelegate(emptyList())

            return WrapperDelegate(
                (expr as IrVararg).elements.map {
                    @Suppress("UNCHECKED_CAST")
                    (it as IrConst<T>).value
                }
            )
        }
    }

    inner class SetVarargFieldDelegate<T> : PropertyDelegateProvider<Any?, WrapperDelegate<Set<T>>> {
        override fun provideDelegate(thisRef: Any?, property: KProperty<*>): WrapperDelegate<Set<T>> {
            val name = property.name
            val expr = arguments[name] ?: return WrapperDelegate(emptySet())

            return WrapperDelegate(
                (expr as IrVararg).elements.map {
                    @Suppress("UNCHECKED_CAST")
                    (it as IrConst<T>).value
                }.toSet()
            )
        }
    }

    protected fun <T> field(default: T? = null) = FieldDelegate(default)
    protected fun classField() = ClassFieldDelegate()
    protected fun <T> optionalField() = OptionalFieldDelegate<T>()
    protected fun <T> varargField() = VarargFieldDelegate<T>()
    protected fun <T> varargSetField() = SetVarargFieldDelegate<T>()

    companion object {
        val knownAnnotations = KrosstalkAnnotation::class.sealedSubclasses
            .associateBy { FqName("$annotationPackage.${it.simpleName!!}") }
            .mapValues { (_, klass) ->
                val constructor = klass.constructors.single()
                val call: (IrConstructorCall) -> KrosstalkAnnotation = {
                    constructor.call(it)
                }
                call
            }

        operator fun invoke(call: IrConstructorCall): KrosstalkAnnotation {
            val constructedName = call.symbol.owner.constructedClass.fqNameForIrSerialization
            val constructor = knownAnnotations[constructedName]
                ?: error("No krosstalk annotation with name $constructedName")
            return constructor(call)
        }
    }

    class KrosstalkMethod(call: IrConstructorCall) : KrosstalkAnnotation(call, "KrosstalkMethod") {
        val klass by classField()
        val noParamHash by field(false)
    }

    class KrosstalkEndpoint(call: IrConstructorCall) :
        KrosstalkAnnotation(call, "KrosstalkEndpoint") {
        val endpoint: String by field(defaultEndpoint)
        val httpMethod: String by field(defaultEndpointHttpMethod)
        val contentType: String by field("")
    }

    class EmptyBody(call: IrConstructorCall) : KrosstalkAnnotation(call, "EmptyBody")

    class ExplicitResult(call: IrConstructorCall) : KrosstalkAnnotation(call, "ExplicitResult")

    class ExceptionHandling(call: IrConstructorCall) : KrosstalkAnnotation(call, "ExceptionHandling") {
        val includeStacktrace by field(false)
        val propagateServerExceptions by field(true)
    }

    class Optional(call: IrConstructorCall) : KrosstalkAnnotation(call, "Optional")

    class PassObjects(call: IrConstructorCall) : KrosstalkAnnotation(call, "PassObjects") {
        val returnToo by field(false)
    }

    class RespondWithHeaders(call: IrConstructorCall) : KrosstalkAnnotation(call, "RespondWithHeaders")

    class ServerURL(call: IrConstructorCall) : KrosstalkAnnotation(call, "ServerURL")

    class RequestHeaders(call: IrConstructorCall) : KrosstalkAnnotation(call, "RequestHeaders")

    class Ignore(call: IrConstructorCall) : KrosstalkAnnotation(call, "Ignore")
}