package com.rnett.krosstalk.metadata

import com.rnett.krosstalk.server.KrosstalkMethodNotFoundException
import kotlin.reflect.KType

@Suppress("unused")
public class KrosstalkSpec<T>(
    public val fullyQualifiedName: String,
    public val shortName: String,
    methodTypes: Map<String, MethodTypes>
) {
    public val methods: Map<String, KrosstalkMethod> = methodTypes.mapValues {
        KrosstalkMethod(this, it.key, it.value.parameters, it.value.returnType)
    }

    public fun findMethod(name: String): KrosstalkMethod? = methods[name]
    public fun method(name: String): KrosstalkMethod = findMethod(name) ?: throw KrosstalkMethodNotFoundException(
        fullyQualifiedName,
        name
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as KrosstalkSpec<*>

        if (fullyQualifiedName != other.fullyQualifiedName) return false
        if (shortName != other.shortName) return false
        if (methods != other.methods) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fullyQualifiedName.hashCode()
        result = 31 * result + shortName.hashCode()
        result = 31 * result + methods.hashCode()
        return result
    }
}

public data class KrosstalkMethod(
    val spec: KrosstalkSpec<*>,
    val methodName: String,
    val parameters: Map<String, ParameterType>,
    val returnType: KType
) {
    val methodFullyQualifiedName: String = spec.fullyQualifiedName + "." + methodName
}