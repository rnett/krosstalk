package com.rnett.krosstalk.endpoint

import com.rnett.krosstalk.*

internal val valueRegex = Regex("\\{([^}]+?)\\}")
internal val optionalRegex = Regex("\\[(\\w+):([^\\]]+?)\\]")
internal val paramRegex = Regex("\\{\\{(\\??)([^}]+?)\\}\\}")


/**
 * An unknown parameter was used in an `@KrosstalkEndpoint` template.
 */
@OptIn(InternalKrosstalkApi::class)
class EndpointUnknownArgumentException @InternalKrosstalkApi constructor(
    val methodName: String,
    val endpointTemplate: Endpoint,
    val missingParam: String,
    val knownParams: Set<String>,
) : KrosstalkException(
    "Endpoint template \"$endpointTemplate\" for method $methodName used parameter $missingParam, but it is not a known parameter.  Known parameters: $knownParams."
)

internal sealed class EndpointPreprocessor {
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
 * * static text
 * * `{var}` -> replace w/ value
 * * `{{var}}` -> replace w/ `/var/{var}` or `&var={var}` if in query params
 * * `[?var:...]` -> body if var != null, else empty.  The body must contain whole segments
 * * `{{?var}}` -> `[?var:{{var}}]`
 */
data class Endpoint(
    val urlParts: EndpointRegion.UrlParts,
    val queryParameters: EndpointRegion.QueryParameters
) {
    companion object {

        private fun parseApparentStatic(value: String): EndpointPart<EndpointRegion> =
            when (value) {
                extensionReceiver -> EndpointPart.Parameter(extensionReceiver)
                instanceReceiver -> EndpointPart.Parameter(instanceReceiver)
                methodName -> EndpointPart.Parameter(methodName)
                krosstalkPrefix -> EndpointPart.Parameter(krosstalkPrefix)
                else -> EndpointPart.Static(value)
            }

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
                        ?: parseApparentStatic(part)
                }
            }

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
                        ?: parseApparentStatic(value)
                }
            }

            return EndpointRegion(parts)
        }

        @InternalKrosstalkApi
        fun withoutStatic(template: String): Endpoint {
            val splitQuestionmark = Regex("(?<![{\\[])\\?").find(template)?.range?.endInclusive ?: template.length

            val urlParts = template.substring(0, splitQuestionmark)
                .trim('/')
                .let { EndpointPreprocessor.preProcessUrlParts(it) }
                .let { parseUrlParts(it) }

            val queryParameters = template.substring(splitQuestionmark)
                .substringAfter("?", "")
                .let { EndpointPreprocessor.preProcessQueryParams(it) }
                .let { parseQueryParameters(it) }

            return Endpoint(urlParts, queryParameters)
        }

        @OptIn(InternalKrosstalkApi::class)
        operator fun invoke(template: String, methodName: String, prefix: String): Endpoint {
            return withoutStatic(template).withStatic(methodName, prefix)
        }
    }

    /**
     * The resolve tree for this endpoint.
     */
    val resolveTree by lazy { EndpointResolveTree(this) }

    /**
     * All possible resolution paths of this endpoint.
     */
    val allResolvePaths by lazy { resolveTree.enumerate() }

    /**
     * Resolve this endpoint.  Returns parameter values if successful, or `null` if not.
     */
    fun resolve(url: UrlRequest) = resolveTree.resolve(url)

    /**
     * Resolve this endpoint.  Returns parameter values if successful, or `null` if not.
     */
    fun resolve(url: String): Map<String, String>? {
        return resolve(UrlRequest(url))
    }

    override fun toString(): String {
        val queryParameters = queryParameters.toString()

        return if (queryParameters.isNotBlank())
            "$urlParts?$queryParameters"
        else
            urlParts.toString()
    }

    @PublishedApi
    internal inline fun mapUrlParts(transform: (EndpointUrlPart) -> EndpointUrlPart?) =
        Endpoint(EndpointRegion(urlParts.mapNotNull(transform)), queryParameters)

    @PublishedApi
    internal inline fun mapQueryParameters(transform: (String, EndpointQueryParameter) -> EndpointQueryParameter?) =
        Endpoint(urlParts, EndpointRegion(queryParameters.mapValues {
            transform(it.key, it.value)
        }.filterValues { it != null } as Map<String, EndpointQueryParameter>))

    @PublishedApi
    internal inline fun mapParts(transform: (EndpointPart<*>) -> EndpointPart<*>?) =
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

    inline fun forEachPart(noinline traverseOptionals: (String) -> Boolean, block: (EndpointPart<*>) -> Unit) {
        urlParts.forEach {
            it.parts(traverseOptionals).forEach(block)
        }
        queryParameters.values.forEach {
            it.parts(traverseOptionals).forEach(block)
        }
    }

    /**
     * Substitute parameters in this endpoint, without resolving optionals
     */
    inline fun fillParameters(transform: (EndpointPart.Parameter) -> String?) =
        mapParts {
            it.mapInOptional {
                if (it is EndpointPart.Parameter)
                    transform(it)?.let { EndpointPart.Static(it) } ?: it
                else
                    it
            }
        }

    /**
     * Include or exclude any optionals depending on if their key is in [usedOptionals]
     */
    @InternalKrosstalkApi
    fun resolveOptionals(usedOptionals: Set<String>) =
        mapParts {
            var part = it
            while (part is EndpointPart.Optional && part.key in usedOptionals)
                part = part.part

            if (part is EndpointPart.Optional)
                null
            else
                part
        }

    @OptIn(InternalKrosstalkApi::class)
    @PublishedApi
    internal inline fun fill(usedOptionals: Set<String>, fillParameter: (key: String) -> String): String {
        return resolveOptionals(usedOptionals).fillParameters { fillParameter(it.param) }.toString()
    }

    /**
     * Substitute static variables (method name and krosstalk prefix) into the endpoint template, and replace parameters.
     * [newParameter] is called on non- [methodName] or [prefix] keys, and like [fill] replaces the entire parameter, curly braces and all.
     * For no change, use the default [newParameter] of ` { "{$it}" }`.
     *
     * For example, to change from curly braces to parentheses you could use `fillEndpointWithStaticAndAdjustParameters(..., { "($it)" })`.
     *
     * **Note that other `fillEndpoint` methods and `splitEndpoint` will only work with keys wrapped in curly braces.**
     *
     * @see [fill]
     */
    internal fun withStatic(
        methodName: String,
        prefix: String,
    ) = fillParameters {
        when (it.param) {
            com.rnett.krosstalk.methodName -> methodName
            com.rnett.krosstalk.krosstalkPrefix -> prefix
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
    @OptIn(InternalKrosstalkApi::class)
    internal fun fillWithArgs(
        methodName: String,
        arguments: Map<String, String>,
        nonNullArguments: Set<String>,
    ): String {
        return fill(nonNullArguments) {
            when (it) {
                com.rnett.krosstalk.methodName -> error("Unresolved method name parameter")
                krosstalkPrefix -> error("Unresolved method name parameter")
                in arguments -> arguments.getValue(it)
                else -> throw EndpointUnknownArgumentException(methodName, this, it, arguments.keys)
            }
        }
    }

    /**
     * Substitute argument values into an endpoint template.  Requires all keys in the template to be used.
     *
     * @see [fill]
     *
     * @return The filled endpoint, and the parameters used to fill it (not including the method name or prefix).
     */
    @OptIn(InternalKrosstalkApi::class)
    inline fun fillWithArgs(
        methodName: String,
        knownArguments: Set<String>,
        usedOptionals: Set<String>,
        getValue: (String) -> String,
    ): Pair<String, Set<String>> {
        val cache = mutableMapOf<String, String>()
        return fill(usedOptionals) {
            when (it) {
                com.rnett.krosstalk.methodName -> error("Unresolved method name parameter")
                krosstalkPrefix -> error("Unresolved method name parameter")
                in knownArguments -> cache.getOrPut(it) { getValue(it) }
                else -> throw EndpointUnknownArgumentException(methodName, this, it, knownArguments)
            }
        } to cache.keys
    }

    @InternalKrosstalkApi
    fun allReferencedParameters(): Set<String> {
        val params = mutableSetOf<String>()
        forEachPart {
            if (it is EndpointPart.Parameter)
                params += it.param
            if (it is EndpointPart.Optional)
                params += it.key
        }
        return params
    }
}

sealed class EndpointRegion {
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
    abstract fun resolveOptionals(taken: Set<String>, untaken: Set<String>): EndpointPart<L>?

    data class Parameter(val param: String) : EndpointPart<EndpointRegion>() {
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
            when (key) {
                in taken -> part.resolveOptionals(taken, untaken)
                in untaken -> null
                else -> part.resolveOptionals(taken, untaken)?.let { copy(key = key, part = it) }
            }
    }

    val allParts: List<EndpointPart<L>>
        get() = when (this) {
            is Parameter -> listOf(this)
            is Static -> listOf(this)
            is Optional -> listOf(this) + part.allParts
        }

    fun parts(traverseOptionals: (String) -> Boolean): List<EndpointPart<L>> =
        if (this is Optional) {
            listOf(this) + if (traverseOptionals(key))
                part.parts(traverseOptionals)
            else
                emptyList()
        } else
            allParts
}

@PublishedApi
internal inline fun <L : EndpointRegion> EndpointPart<L>.mapInOptional(transform: (EndpointPart<L>) -> EndpointPart<L>): EndpointPart<L> =
    when (this) {
        is EndpointPart.Parameter -> transform(this)
        is EndpointPart.Static -> transform(this)
        is EndpointPart.Optional -> copy(part = transform(part))
    }