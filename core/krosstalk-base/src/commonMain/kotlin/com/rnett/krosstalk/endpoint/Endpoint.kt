package com.rnett.krosstalk.endpoint

import com.rnett.krosstalk.InternalKrosstalkApi
import com.rnett.krosstalk.KrosstalkException
import com.rnett.krosstalk.KrosstalkPluginApi
import com.rnett.krosstalk.extensionReceiver
import com.rnett.krosstalk.instanceReceiver
import com.rnett.krosstalk.krosstalkPrefix
import com.rnett.krosstalk.methodName

internal val valueRegex = Regex("\\{([^}]+?)\\}")
internal val optionalRegex = Regex("\\[(\\w+):([^\\]]+?)\\]")
internal val paramRegex = Regex("\\{\\{(\\??)([^}]+?)\\}\\}")


/**
 * An unknown parameter was used in an `@KrosstalkEndpoint` template.
 */
@OptIn(InternalKrosstalkApi::class, KrosstalkPluginApi::class)
public class EndpointUnknownArgumentException @OptIn(KrosstalkPluginApi::class)
@InternalKrosstalkApi constructor(
    public val methodName: String,
    public val endpointTemplate: Endpoint,
    public val missingParam: String,
    public val knownParams: Set<String>,
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

        fun preProcessUrlParts(text: String) = preprocessors.fold(text) { all, it -> it.preProcessUrlParts(all) }
        fun preProcessQueryParams(text: String) = preprocessors.fold(text) { all, it -> it.preProcessQueryParams(all) }
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
@KrosstalkPluginApi
public data class Endpoint(
    val urlParts: EndpointRegion.UrlParts,
    val queryParameters: EndpointRegion.QueryParameters,
) {
    @KrosstalkPluginApi
    public companion object {

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
        public fun withoutStatic(template: String): Endpoint {
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
        public operator fun invoke(template: String, methodName: String, prefix: String): Endpoint {
            return withoutStatic(template).withStatic(methodName, prefix)
        }
    }

    /**
     * The resolve tree for this endpoint.
     */
    val resolveTree: EndpointResolveTree by lazy { EndpointResolveTree(this) }

    /**
     * All possible resolution paths of this endpoint.
     */
    val allResolvePaths: Set<ResolveEndpoint> by lazy { resolveTree.enumerate() }

    /**
     * Resolve this endpoint.  Returns parameter values if successful, or `null` if not.
     */
    public fun resolve(url: UrlRequest): Map<String, String>? = resolveTree.resolve(url)

    /**
     * Resolve this endpoint.  Returns parameter values if successful, or `null` if not.
     */
    public fun resolve(url: String): Map<String, String>? {
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
    internal inline fun mapUrlParts(transform: (EndpointUrlPart) -> EndpointUrlPart?): Endpoint =
        Endpoint(EndpointRegion(urlParts.mapNotNull(transform)), queryParameters)

    @PublishedApi
    internal inline fun mapQueryParameters(transform: (String, EndpointQueryParameter) -> EndpointQueryParameter?): Endpoint =
        Endpoint(urlParts, EndpointRegion(
            queryParameters.mapValues {
                transform(it.key, it.value)
            }.filterValues { it != null }.let {
                @Suppress("UNCHECKED_CAST")
                it as Map<String, EndpointQueryParameter>
            })
        )

    @PublishedApi
    internal inline fun mapParts(transform: (EndpointPart<*>) -> EndpointPart<*>?): Endpoint =
        mapUrlParts {
            @Suppress("UNCHECKED_CAST")
            transform(it) as EndpointUrlPart?
        }
            .mapQueryParameters { _, it ->
                @Suppress("UNCHECKED_CAST")
                transform(it) as EndpointQueryParameter?
            }


    /**
     * Call [block] on each part of the endpoint, traversing down into optionals if [traverseOptionals] is true.
     *
     * Calls for the optional before going into it.
     */
    public inline fun forEachPart(traverseOptionals: Boolean = true, block: (EndpointPart<*>) -> Unit) {
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
     * Call [block] on each part of the endpoint, traversing down into optionals if [traverseOptionals] is true for that optional.
     *
     * Calls for the optional before going into it.
     */
    public inline fun forEachPart(noinline traverseOptionals: (String) -> Boolean, block: (EndpointPart<*>) -> Unit) {
        urlParts.forEach {
            it.parts(traverseOptionals).forEach(block)
        }
        queryParameters.values.forEach {
            it.parts(traverseOptionals).forEach(block)
        }
    }

    /**
     * Substitute parameters in this endpoint, without resolving optionals (it maps into all of them)
     */
    public inline fun fillParameters(transform: (EndpointPart.Parameter) -> String?): Endpoint =
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
    public fun resolveOptionals(usedOptionals: Set<String>): Endpoint =
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
    public inline fun fillWithArgs(
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
    public fun allReferencedParameters(): Set<String> {
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

@KrosstalkPluginApi
public sealed class EndpointRegion {
    public data class UrlParts(val parts: List<EndpointUrlPart>) : EndpointRegion(), List<EndpointPart<UrlParts>> by parts {
        override fun toString(): String = parts.joinToString("/")
    }

    public data class QueryParameters(val parameters: Map<String, EndpointQueryParameter>) : EndpointRegion(),
        Map<String, EndpointPart<QueryParameters>> by parameters {
        override fun toString(): String = parameters.entries.joinToString("&") { (key, value) -> "$key=$value" }
    }

    public companion object {
        public operator fun invoke(parts: List<EndpointUrlPart>): UrlParts = UrlParts(parts)
        public operator fun invoke(parameters: Map<String, EndpointQueryParameter>): QueryParameters = QueryParameters(parameters)
    }
}

@KrosstalkPluginApi
public typealias EndpointUrlPart = EndpointPart<EndpointRegion.UrlParts>

@KrosstalkPluginApi
public typealias EndpointQueryParameter = EndpointPart<EndpointRegion.QueryParameters>

@KrosstalkPluginApi
public sealed class EndpointPart<in L : EndpointRegion> {
    public abstract fun resolveOptionals(taken: Set<String>, untaken: Set<String>): EndpointPart<L>?

    public data class Parameter(val param: String) : EndpointPart<EndpointRegion>() {
        override fun toString(): String = "{$param}"
        override fun resolveOptionals(taken: Set<String>, untaken: Set<String>): EndpointPart<EndpointRegion> = this
    }

    public data class Static(val part: String) : EndpointPart<EndpointRegion>() {
        override fun toString(): String = part
        override fun resolveOptionals(taken: Set<String>, untaken: Set<String>): EndpointPart<EndpointRegion> = this
    }

    public data class Optional<L : EndpointRegion>(val key: String, val part: EndpointPart<L>) : EndpointPart<L>() {
        override fun toString(): String = "[?$key:$part]"
        override fun resolveOptionals(taken: Set<String>, untaken: Set<String>): EndpointPart<L>? =
            when (key) {
                in taken -> part.resolveOptionals(taken, untaken)
                in untaken -> null
                else -> part.resolveOptionals(taken, untaken)?.let { copy(key = key, part = it) }
            }
    }

    public val allParts: List<EndpointPart<L>>
        get() = when (this) {
            is Parameter -> listOf(this)
            is Static -> listOf(this)
            is Optional -> listOf(this) + part.allParts
        }

    public fun parts(traverseOptionals: (String) -> Boolean): List<EndpointPart<L>> =
        if (this is Optional) {
            listOf(this) + if (traverseOptionals(key))
                part.parts(traverseOptionals)
            else
                emptyList()
        } else
            allParts
}

@KrosstalkPluginApi
@PublishedApi
internal inline fun <L : EndpointRegion> EndpointPart<L>.mapInOptional(transform: (EndpointPart<L>) -> EndpointPart<L>): EndpointPart<L> =
    when (this) {
        is EndpointPart.Parameter -> transform(this)
        is EndpointPart.Static -> transform(this)
        is EndpointPart.Optional -> copy(part = transform(part))
    }