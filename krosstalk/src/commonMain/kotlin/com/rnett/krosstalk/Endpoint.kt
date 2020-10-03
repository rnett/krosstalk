package com.rnett.krosstalk

@PublishedApi
internal val valueRegex = Regex("(\\{[^}]+?\\})")

//TODO strongly type endpoint more, maybe use List<EndpointPart> as the default?
//  could have wrap lambda to wrap keys and not depend on {/} except for initial parsing

inline class EndpointTemplate(val template: String) {
    override fun toString(): String = template
}

sealed class EndpointPart {
    data class Parameter(val key: String) : EndpointPart() {
        val isMethodName = key == methodNameKey
        val isPrefix = key == prefixKey
        val isExtensionReceiver = key == extensionParameterKey
        val isInstanceReceiver = key == instanceParameterKey

        override fun toString(): String = "{$key}"
    }

    data class Static(val part: String) : EndpointPart() {
        override fun toString(): String = part
    }
}

fun Iterable<EndpointPart>.toEndpointTemplate() = EndpointTemplate(joinToString("/"))

/**
 * Split an endpoint template into its parts
 */
fun splitEndpoint(endpointTemplate: EndpointTemplate): List<EndpointPart> {
    val parts = endpointTemplate.template.trim('/').split('/')
    return parts.map {
        if (valueRegex.matchEntire(it) != null)
            EndpointPart.Parameter(it.substring(1, it.length - 1))
        else
            EndpointPart.Static(it)
    }
}

/**
 * Helper to fill in endpoint templated.
 * [fill] will be called on each key, i.e. the thing inside the curly braces, but replaces the curly braces too.
 *
 * `"{test}/{test2}"` would have [fill] called with "test" and "test2", and it [fill] returned the keys, would become `"test/test2"`.
 *
 */
inline fun EndpointTemplate.fill(
    crossinline fill: (key: String) -> String
) =
    template.replace(valueRegex) {
        fill(it.groupValues[1].let {
            it.substring(1, it.length - 1)
        })
    }

/**
 * Substitute static variables (method name and krosstalk prefix) into the endpoint template, and replace parameters.
 * [newParameter] is called on non- [methodNameKey] or [prefixKey] keys, and like [fill] replaces the entire parameter, curly braces and all.
 * For no change, use the default [newParameter] of ` { "{$it}" }`.
 *
 * For example, to change from curly braces to parentheses you could use `fillEndpointWithStaticAndAdjustParameters(..., { "($it)" })`.
 *
 * **Note that other `fillEndpoint` methods and `splitEndpoint` will only work with keys wrapped in curly braces.**
 *
 * @see [fill]
 */
inline fun EndpointTemplate.fillWithStaticAndAdjustParameters(
    methodName: String,
    prefix: String,
    crossinline newParameter: (String) -> String = { "{$it}" }
) = fill {
    when (it) {
        methodNameKey -> methodName
        prefixKey -> prefix
        else -> newParameter(it)
    }
}

/**
 * Substitute static variables (method name and krosstalk prefix) into the endpoint template.
 *
 * @see [fillWithStaticAndAdjustParameters]
 */
fun EndpointTemplate.fillWithStatic(methodName: String, prefix: String) =
    fillWithStaticAndAdjustParameters(methodName, prefix)


/**
 * Substitute argument values into an endpoint template.  Requires all keys in the template to be used.
 *
 * @see [fill]
 */
fun EndpointTemplate.fillWithArgs(
    methodName: String,
    prefix: String,
    arguments: Map<String, *>
) = fill {
    when (it) {
        methodNameKey -> methodName
        prefixKey -> prefix
        in arguments -> arguments[it].toString()
        else -> throw KrosstalkException.EndpointUnknownArgument(methodName, this, it, arguments.keys)
    }
}