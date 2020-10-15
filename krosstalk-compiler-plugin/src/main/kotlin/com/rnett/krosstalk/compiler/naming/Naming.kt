package com.rnett.krosstalk.compiler.naming

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import kotlin.reflect.KProperty


class WrapperDelegate<T>(val value: T) {
    inline operator fun getValue(thisRef: Any?, property: KProperty<*>) = value
}

class NameWrapperDelegate<T>(val build: (String) -> T) {
    inline operator fun provideDelegate(thisRef: Any?, property: KProperty<*>) = WrapperDelegate(build(property.name))
}

interface HasName {
    val fqName: FqName
    val parent: HasName?

    operator fun String.unaryMinus() = Package()
    operator fun String.unaryPlus() = Class()

    fun String.Package() = PackageRef(this, this@HasName)
    fun String.Class() = ClassRef(this, this@HasName)

    fun function(filter: (IrSimpleFunctionSymbol) -> Boolean = { true }) = NameWrapperDelegate { FunctionRef(it, this, filter) }
    fun property(filter: (IrPropertySymbol) -> Boolean = { true }) = NameWrapperDelegate { PropertyRef(it, this, filter) }
    fun Class() = NameWrapperDelegate { ClassRef(it, this) }

    fun function(name: String, filter: (IrSimpleFunctionSymbol) -> Boolean = { true }) = WrapperDelegate(FunctionRef(name, this, filter))
    fun property(name: String, filter: (IrPropertySymbol) -> Boolean = { true }) = WrapperDelegate(PropertyRef(name, this, filter))
    fun Class(name: String) = WrapperDelegate(ClassRef(name, this))
}

abstract class RootPackage(override val fqName: FqName = FqName.ROOT) : HasName {
    override val parent: HasName? = null

    constructor(fqName: String) : this(FqName(fqName))
}

interface Reference<R : IrBindableSymbol<*, *>> : HasName/*, IrBindableSymbol<D, B>*/ {
    fun get(context: IrPluginContext): R
}

fun FqName.descendant(id: String): FqName =
    id.split('.')
        .fold(this) { name, part ->
            name.child(Name.guessByFirstCharacter(part))
        }


abstract class BaseRef(val name: String, override val parent: HasName) : HasName {
    override val fqName: FqName get() = (parent.fqName).descendant(name)
}

interface Package : HasName

class PackageRef(name: String, parent: HasName) : BaseRef(name, parent), Package

val IrClassSymbol.primaryConstructor get() = constructors.singleOrNull { it.owner.isPrimary }

interface Class : Reference<IrClassSymbol> {
    override fun get(context: IrPluginContext) = context.referenceClass(fqName)!!

    fun constructor(filter: (IrConstructorSymbol) -> Boolean) = NameWrapperDelegate { ConstructorRef(it, this, filter) }
    fun primaryConstructor() = constructor { it.owner.isPrimary }

}

class ClassRef(name: String, parent: HasName) : BaseRef(name, parent), Class

class FunctionRef(name: String, parent: HasName, val filter: (IrSimpleFunctionSymbol) -> Boolean) :
    BaseRef(name, parent), Reference<IrSimpleFunctionSymbol> {
    override fun get(context: IrPluginContext) = context.referenceFunctions(fqName).single(filter)
}

class PropertyRef(name: String, parent: HasName, val filter: (IrPropertySymbol) -> Boolean) :
    BaseRef(name, parent), Reference<IrPropertySymbol> {
    override fun get(context: IrPluginContext) = context.referenceProperties(fqName).single(filter)
}

class ConstructorRef(name: String, override val parent: Class, val filter: (IrConstructorSymbol) -> Boolean) :
    BaseRef(name, parent), Reference<IrConstructorSymbol> {
    override fun get(context: IrPluginContext) = context.referenceConstructors(fqName).single(filter)
}
