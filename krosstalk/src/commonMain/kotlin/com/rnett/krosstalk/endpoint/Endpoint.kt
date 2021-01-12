package com.rnett.krosstalk.endpoint

import com.rnett.krosstalk.*
import kotlin.math.pow

internal val valueRegex = Regex("\\{([^}]+?)\\}")
internal val optionalRegex = Regex("\\[(\\w+):([^\\]]+?)\\]")
internal val paramRegex = Regex("\\{\\{(\\??)([^}]+?)\\}\\}")

//TODO more complex endpoint stuff.  optional arguments, optional keys (i.e. "/id/{id}" onlt if id is != null), etc.
//  needs support for server side default args?  (i.e. don't send and use default) I think this is actually supported in `call`


sealed class EndpointPreprocessor {
    fun preProcessUrlParts(text: String): String = preProcess(text, false)
    fun preProcessQueryParams(text: String): String = preProcess(text, true)

    abstract fun preProcess(text: String, isQueryParams: Boolean): String

    object FullParamPreprocessor : EndpointPreprocessor() {
        override fun preProcess(text: String, isQueryParams: Boolean): String {
            return text.replace(paramRegex) {
                val optional = it.groupValues[1] == "?"
                val param = it.groupValues[2]

                val inner = if (isQueryParams) "$param={$param}" else "$param/{$param}"
                if (optional)
                    "[?$param:$inner]"
                else
                    inner
            }
        }

    }

    companion object {
        val preprocessors = listOf<EndpointPreprocessor>(FullParamPreprocessor)

        fun preProcessUrlParts(text: String) = preprocessors.fold(text) { text, it -> it.preProcessUrlParts(text) }
        fun preProcessQueryParams(text: String) = preprocessors.fold(text) { text, it -> it.preProcessQueryParams(text) }
    }
}

/**
 * Class representing an endpoint template.
 * Valid template structures:
 * {var} -> replace w/ value
 * {{var}} -> replace w/ /var/{var} or &var={var}
 * [?var:...] -> if var != null.  Must contain whole segments
 * {{?var}} -> [?var:{{var}}]
 */
data class Endpoint(
    val urlParts: EndpointRegion.UrlParts,
    val queryParameters: EndpointRegion.QueryParameters
) {
    companion object {

        private fun parseUrlParts(template: String): EndpointRegion.UrlParts {

            val parts = mutableListOf<EndpointPart<EndpointRegion.UrlParts>>()
            var left = template.trim('/')

            while (left.isNotEmpty()) {
                left = left.trim('/')

                if (left.startsWith("[?")) {
                    var optionalBlockEnd = 2
                    var count = 1
                    while (count != 0) {
                        if (left[optionalBlockEnd] == '[')
                            count++
                        else if (left[optionalBlockEnd] == ']')
                            count--

                        optionalBlockEnd++
                    }

                    val optionalBlock = left.substring(2, optionalBlockEnd - 1)
                    left = left.substring(optionalBlockEnd)
                    val param = optionalBlock.substringBefore(':')
                    parts += parseUrlParts(optionalBlock.substringAfter(':')).map { EndpointPart.Optional(param, it) }
                } else {
                    val part = left.substringBefore('/')
                    left = left.substringAfter('/', "")

                    parts += valueRegex.matchEntire(part)?.let { EndpointPart.Parameter(it.groupValues[1]) }
                        ?: EndpointPart.Static(part)
                }
            }


//            template
//                .trim('/')
//                .split('/')
//                .flatMap {
//                    valueRegex.matchEntire(it)?.let { listOf(EndpointPart.Parameter(it.groupValues[1])) }
//                        ?: optionalRegex.matchEntire(it)?.let {
//                            val param = it.groupValues[1]
//                            parseUrlParts(it.groupValues[2]).map { EndpointPart.Optional(param, it) }
//                        }
//                        ?: listOf(EndpointPart.Static(it))
//                }

            return EndpointRegion(parts)
        }

        private fun parseQueryParameters(template: String): EndpointRegion.QueryParameters {
            if (template.isBlank())
                return EndpointRegion(emptyMap())

            val parts = mutableMapOf<String, EndpointPart<EndpointRegion.QueryParameters>>()
            var left = template.trim('?', '&')

            while (left.isNotEmpty()) {
                left = left.trim('?', '&')

                if (left.startsWith("[?")) {
                    var optionalBlockEnd = 2
                    var count = 1
                    while (count != 0) {
                        if (left[optionalBlockEnd] == '[')
                            count++
                        else if (left[optionalBlockEnd] == ']')
                            count--

                        optionalBlockEnd++
                    }

                    val optionalBlock = left.substring(2, optionalBlockEnd - 1)
                    left = left.substring(optionalBlockEnd)
                    val param = optionalBlock.substringBefore(':')
                    parts += parseQueryParameters(optionalBlock.substringAfter(':')).mapValues { EndpointPart.Optional(param, it.value) }
                } else {
                    val part = left.substringBefore('&')
                    left = left.substringAfter('&', "")

                    val key = part.substringBefore("=")
                    val value = part.substringAfter("=")

                    parts[key] = valueRegex.matchEntire(value)?.let { EndpointPart.Parameter(it.groupValues[1]) }
                        ?: EndpointPart.Static(value)
                }
            }

//            template
//                .split('&')
//                .flatMap {
//                    optionalRegex.matchEntire(it)?.let {
//                        val param = it.groupValues[1]
//                        parseQueryParameters(it.groupValues[2]).map {
//                            it.key to EndpointPart.Optional(param, it.value)
//                        }
//                    } ?: listOf(kotlin.run {
//                        val key = it.substringBefore("=")
//                        val value = it.substringAfter("=")
//                        key to (valueRegex.matchEntire(value)?.let { EndpointPart.Parameter(it.groupValues[1]) }
//                            ?: EndpointPart.Static(value))
//                    })
//
//                }.toMap()

            return EndpointRegion(parts)
        }

        operator fun invoke(template: String, methodName: String, prefix: String): Endpoint {

            val splitQuestionmark = Regex("(?<![{\\[])\\?").find(template)?.range?.endInclusive ?: template.length

            val urlParts = template.substring(0, splitQuestionmark)
                .trim('/')
                .let { EndpointPreprocessor.preProcessUrlParts(it) }
                .let { parseUrlParts(it) }

            val queryParameters = template.substring(splitQuestionmark)
                .substringAfter("?", "")
                .let { EndpointPreprocessor.preProcessQueryParams(it) }
                .let { parseQueryParameters(it) }

            return Endpoint(urlParts, queryParameters).withStatic(methodName, prefix)
        }
    }

    val resolveTree by lazy { EndpointResolveTree(this) }
    val possibleEndpoints by lazy { resolveTree.enumerate() }

    fun resolve(url: UrlRequest) = resolveTree.resolve(url)
    fun resolve(url: String) = resolve(UrlRequest(url))

    //TODO make this work with optionals.  Preferablly don't enumerate, may have to.
    fun extractParameters(url: String): Map<String, String>? {
        return resolveTree.resolve(UrlRequest(url))
    }

    override fun toString(): String {
        val queryParameters = queryParameters.toString()

        return if (queryParameters.isNotBlank())
            "$urlParts?$queryParameters"
        else
            urlParts.toString()
    }

    inline fun mapUrlParts(transform: (EndpointUrlPart) -> EndpointUrlPart?) =
        Endpoint(EndpointRegion(urlParts.mapNotNull(transform)), queryParameters)

    inline fun mapQueryParameters(transform: (String, EndpointQueryParameter) -> EndpointQueryParameter?) =
        Endpoint(urlParts, EndpointRegion(queryParameters.mapValues {
            transform(it.key, it.value)
        }.filterValues { it != null } as Map<String, EndpointQueryParameter>))

    inline fun mapParts(transform: (EndpointPart<*>) -> EndpointPart<*>?) =
        mapUrlParts { transform(it) as EndpointUrlPart? }
            .mapQueryParameters { _, it -> transform(it) as EndpointQueryParameter? }

    inline fun forEachPart(traverseOptionals: Boolean = true, block: (EndpointPart<*>) -> Unit) {
        urlParts.forEach {
            if (traverseOptionals)
                it.allParts.forEach(block)
            else
                block(it)
        }
        queryParameters.values.forEach {
            if (traverseOptionals)
                it.allParts.forEach(block)
            else
                block(it)
        }
    }

    /**
     * Substitute parameters in this endpoint
     */
    inline fun fillParameters(crossinline transform: (EndpointPart.Parameter) -> String?) =
        mapParts {
            it.mapInOptional {
                if (it is EndpointPart.Parameter)
                    transform(it)?.let { EndpointPart.Static(it) } ?: it
                else
                    it
            }
        }

    /**
     * Include or exclude any optionals depending on if their key is in [nonNullArguments]
     */
    fun resolveOptionals(nonNullArguments: Set<String>) =
        mapParts {
            var part = it
            while (part is EndpointPart.Optional && part.key in nonNullArguments)
                part = part.part

            if (part is EndpointPart.Optional)
                null
            else
                part
        }

    /**
     * List endpoints for all possible combinations of null or not null arguments
     */
    //TODO need a more efficient structure.  Tree?  Then I can use it for extraction too.
    fun enumerateOptionals(): Map<Set<String>, Endpoint> {
        val paramSet = mutableSetOf<String>()
        forEachPart {
            if (it is EndpointPart.Optional)
                paramSet += it.key
        }
        val params = paramSet.toList()
        val max = 2f.pow(params.size).toInt()
        return (0 until max).map { filter ->
            params.mapIndexedNotNull { idx: Int, v: String ->
                if ((1 shl idx) and filter != 0)
                    v
                else
                    null
            }.toSet()
        }.map {
            it to resolveOptionals(it)
        }.toMap()
    }

    fun fill(nonNullArguments: Set<String>, fillParameter: (key: String) -> String): String {
        return resolveOptionals(nonNullArguments).fillParameters { fillParameter(it.param) }.toString()
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
    fun withStatic(
        methodName: String,
        prefix: String
    ) = fillParameters {
        when (it.param) {
            methodNameKey -> methodName
            prefixKey -> prefix
            else -> null
        }
    }

    /**
     * Substitute argument values into an endpoint template.  Requires all keys in the template to be used.
     *
     * @see [fill]
     *
     * @return The filled endpoint, and the parameters used to fill it (not including the method name or prefix).
     */
    fun fillWithArgs(
        methodName: String,
        arguments: Map<String, String>,
        nonNullArguments: Set<String>
    ): String {
        return fill(nonNullArguments) {
            when (it) {
                methodNameKey -> error("Unresolved method name parameter")
                prefixKey -> error("Unresolved method name parameter")
                in arguments -> arguments.getValue(it)
                else -> throw KrosstalkException.EndpointUnknownArgument(methodName, this, it, arguments.keys)
            }
        }
    }

    fun usedParameters(nonNullArguments: Set<String>? = null, excludeStatic: Boolean = true): Set<String> {
        val endpoint = nonNullArguments?.let { resolveOptionals(it) } ?: this
        val used = mutableSetOf<String>()

        endpoint.forEachPart {
            if (it is EndpointPart.Parameter)
                used += it.param
        }

        return if (excludeStatic)
            used - setOf(methodNameKey, prefixKey)
        else
            used
    }

    val usedArguments by lazy { usedParameters() }
}

sealed class EndpointRegion {
    fun fill(fillParameter: (key: String) -> String, nonNullArguments: Set<String>): String = when (this) {
        is UrlParts -> parts.mapNotNull { it.fill(fillParameter, nonNullArguments) }.joinToString("/")
        is QueryParameters -> parameters.mapValues { it.value.fill(fillParameter, nonNullArguments) }
            .filterValues { it != null }
            .entries.joinToString("&") { (key, value) -> "$key=$value" }
    }

    data class UrlParts(val parts: List<EndpointUrlPart>) : EndpointRegion(), List<EndpointPart<UrlParts>> by parts {
        override fun toString(): String = parts.joinToString("/")
    }

    data class QueryParameters(val parameters: Map<String, EndpointQueryParameter>) : EndpointRegion(),
        Map<String, EndpointPart<QueryParameters>> by parameters {
        override fun toString(): String = parameters.entries.joinToString("&") { (key, value) -> "$key=$value" }
    }

    companion object {
        operator fun invoke(parts: List<EndpointUrlPart>) = UrlParts(parts)
        operator fun invoke(parameters: Map<String, EndpointQueryParameter>) = QueryParameters(parameters)
    }
}

typealias EndpointUrlPart = EndpointPart<EndpointRegion.UrlParts>
typealias EndpointQueryParameter = EndpointPart<EndpointRegion.QueryParameters>

sealed class EndpointPart<in L : EndpointRegion> {
    fun fill(fillParameter: (key: String) -> String, nonNullArguments: Set<String>): String? = when (this) {
        is Static -> part
        is Parameter -> fillParameter(param)
        is Optional -> if (key in nonNullArguments) part.fill(fillParameter, nonNullArguments) else null
    }

    val paramOrNull: String?
        get() = when (this) {
            is Parameter -> param
            is Static -> null
            is Optional -> part.paramOrNull
        }

    abstract fun resolveOptionals(taken: Set<String>, untaken: Set<String>): EndpointPart<L>?

    data class Parameter(val param: String) : EndpointPart<EndpointRegion>() {
        val isMethodName = param == methodNameKey
        val isPrefix = param == prefixKey
        val isExtensionReceiver = param == extensionParameterKey
        val isInstanceReceiver = param == instanceParameterKey

        override fun toString(): String = "{$param}"
        override fun resolveOptionals(taken: Set<String>, untaken: Set<String>): EndpointPart<EndpointRegion> = this
    }

    data class Static(val part: String) : EndpointPart<EndpointRegion>() {
        override fun toString(): String = part
        override fun resolveOptionals(taken: Set<String>, untaken: Set<String>): EndpointPart<EndpointRegion> = this
    }

    data class Optional<L : EndpointRegion>(val key: String, val part: EndpointPart<L>) : EndpointPart<L>() {
        override fun toString() = "[?$key:$part]"
        override fun resolveOptionals(taken: Set<String>, untaken: Set<String>): EndpointPart<L>? =
            if (key in taken)
                part.resolveOptionals(taken, untaken)
            else if (key in untaken)
                null
            else
                part.resolveOptionals(taken, untaken)?.let { copy(key = key, part = it) }
    }

    val allParts: List<EndpointPart<L>>
        get() = when (this) {
            is Parameter -> listOf(this)
            is Static -> listOf(this)
            is Optional -> listOf(this) + part.allParts
        }
}

fun <L : EndpointRegion> EndpointPart<L>.mapInOptional(transform: (EndpointPart<L>) -> EndpointPart<L>): EndpointPart<L> = when (this) {
    is EndpointPart.Parameter -> transform(this)
    is EndpointPart.Static -> transform(this)
    is EndpointPart.Optional -> copy(part = transform(part))
}