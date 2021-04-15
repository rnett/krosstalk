package com.rnett.krosstalk.endpoint

import com.rnett.krosstalk.krosstalkPrefix
import com.rnett.krosstalk.methodName


internal fun getUrlPath(url: String): String {
    // something like test/url/part or /test/part/2
    val path = if (url.substringBefore("/", "").matches(Regex("[a-z]*", RegexOption.IGNORE_CASE)))
        url
    else
        url.substringAfter("://").substringAfter("/", "")
    return path.substringBefore("#")
}

data class UrlRequest(val urlParts: List<String>, val queryParams: Map<String, String>) {
    constructor(url: String) : this(
        getUrlPath(url).substringBefore("?").split("/").filter { it.isNotBlank() },
        getUrlPath(url).substringAfter("?", "").split("&").filter { it.isNotBlank() }.associate {
            if ("=" !in it)
                error("Malformed url query parameter: $it.  No '=' found.")
            it.substringBefore('=') to it.substringAfter('=')
        }
    )


    fun withoutPrefixParts(prefix: List<String>): UrlRequest {
        val newParts = urlParts.toMutableList()
        prefix.forEach {
            if (newParts.firstOrNull() == it)
                newParts.removeFirst()
            else
                return UrlRequest(newParts, queryParams)
        }
        return UrlRequest(newParts, queryParams)
    }

    fun withoutPrefix(prefix: String): UrlRequest = withoutPrefixParts(prefix.split('/').filter { it.isNotBlank() })

    fun withoutQueryParams(params: Set<String>) = UrlRequest(urlParts, queryParams - params)

    override fun toString(): String {
        return buildString {
            append(urlParts.joinToString("/"))
            if (queryParams.isNotEmpty()) {
                append("?")
                append(queryParams.entries.joinToString("&") { "${it.key}=${it.value}" })
            }
        }
    }
}

internal sealed class ResolveResult {
    object Pass : ResolveResult()
    object Fail : ResolveResult()
    data class AddParam(val param: String, val value: String) : ResolveResult()
}

sealed class ResolveQueryParam {
    abstract val isOptional: Boolean

    data class Static(val value: String, override val isOptional: Boolean) : ResolveQueryParam() {
        override fun toString(): String = if (isOptional) "[?$value]" else value
    }

    data class Param(val param: String, override val isOptional: Boolean) : ResolveQueryParam() {
        init {
            if (param == methodName || param == krosstalkPrefix)
                error("Can't have resolve param of static parameter, use withStatic on the Endpoint first.")
        }

        override fun toString(): String = if (isOptional) "{?$param}" else "{$param}"
    }

    companion object {

        operator fun invoke(part: EndpointQueryParameter): ResolveQueryParam = when (part) {
            is EndpointPart.Parameter -> Param(part.param, false)
            is EndpointPart.Static -> Static(part.part, false)
            is EndpointPart.Optional -> when (val sub = ResolveQueryParam(part.part)) {
                is Static -> sub.copy(isOptional = true)
                is Param -> sub.copy(isOptional = true)
            }

        }

        fun build(taken: Set<String>, untaken: Set<String>, urlParams: Map<String, EndpointQueryParameter>): Map<String, ResolveQueryParam> =
            urlParams.mapNotNull { (key, value) ->
                val resolved = value.resolveOptionals(taken, untaken) ?: return@mapNotNull null
                key to ResolveQueryParam(resolved)
            }.toMap()
    }
}

sealed class ResolveUrlPart {

    internal abstract fun resolve(urlPart: String): ResolveResult

    data class Static(val value: String) : ResolveUrlPart() {
        override fun resolve(urlPart: String): ResolveResult =
            if (urlPart == value)
                ResolveResult.Pass
            else
                ResolveResult.Fail

        override fun toString(): String = value
    }

    data class Param(val param: String) : ResolveUrlPart() {
        init {
            if (param == methodName || param == krosstalkPrefix)
                error("Can't have resolve param of static parameter, use withStatic on the Endpoint first.")
        }

        override fun resolve(urlPart: String): ResolveResult =
            ResolveResult.AddParam(param, urlPart)

        override fun toString(): String = "{$param}"
    }

    companion object {
        operator fun invoke(part: EndpointUrlPart): ResolveUrlPart = when (part) {
            is EndpointPart.Parameter -> Param(part.param)
            is EndpointPart.Static -> Static(part.part)
            is EndpointPart.Optional -> error("Can't resolve Optional")
        }

        fun build(parts: List<EndpointUrlPart>) = parts.map(::invoke)
    }
}

data class ResolveEndpoint(val urlParts: List<ResolveUrlPart>, val queryParams: Map<String, ResolveQueryParam>) {
    internal constructor(endpoint: Endpoint) : this(
        endpoint.urlParts.map { ResolveUrlPart(it) },
        endpoint.queryParameters.mapValues { ResolveQueryParam(it.value) })

    //TODO(low priority) add a resolve method for use with Endpoint.allResolvePaths

    override fun toString(): String {
        return buildString {
            append(urlParts.joinToString("/"))
            if (queryParams.isNotEmpty()) {
                append("?")
                append(queryParams.entries.joinToString("&") { (k, v) -> "$k=$v" })
            }
        }
    }
}

sealed class EndpointResolveTree {
    abstract fun enumerate(): Set<ResolveEndpoint>

    abstract fun resolve(url: UrlRequest): Map<String, String>?

    data class Fork(val options: Set<EndpointResolveTree>) : EndpointResolveTree() {
        override fun enumerate(): Set<ResolveEndpoint> = options.flatMap { it.enumerate() }.toSet()

        override fun resolve(url: UrlRequest): Map<String, String>? {
            options.forEach {
                val resolved = it.resolve(url)
                if (resolved != null)
                    return resolved
            }
            return null
        }
    }

    data class Multiple(val parts: List<ResolveUrlPart>, val node: EndpointResolveTree) : EndpointResolveTree() {
        override fun enumerate(): Set<ResolveEndpoint> = node.enumerate()
        override fun resolve(url: UrlRequest): Map<String, String>? {
            val partsLeft = url.urlParts.toMutableList()
            val params = mutableMapOf<String, String>()

            parts.forEach {
                if (partsLeft.isEmpty())
                    return null

                when (val result = it.resolve(partsLeft.removeFirst())) {
                    ResolveResult.Pass -> {
                    }
                    ResolveResult.Fail -> return null
                    is ResolveResult.AddParam -> {
                        params[result.param] = result.value
                    }
                }
            }

            return node.resolve(url.copy(urlParts = partsLeft))?.plus(params)
        }
    }

    data class Leaf(
        val full: List<ResolveUrlPart>,
        val takenOptions: Set<String>,
        val untakenOptions: Set<String>,
        val queryParams: Map<String, ResolveQueryParam>
    ) :
        EndpointResolveTree() {
        val resolvedEndpoint by lazy {
            ResolveEndpoint(full, queryParams)
        }

        override fun enumerate(): Set<ResolveEndpoint> = setOf(resolvedEndpoint)
        override fun resolve(url: UrlRequest): Map<String, String>? =
            if (url.urlParts.isEmpty()) {
                val params = mutableMapOf<String, String>()
                queryParams.forEach { (key, value) ->
                    if (!value.isOptional && key !in url.queryParams)
                        return null

                    when (value) {
                        is ResolveQueryParam.Static -> {
                            if (key in url.queryParams && value.value != url.queryParams[key])
                                return null
                        }
                        is ResolveQueryParam.Param -> {
                            if (key in url.queryParams)
                                params[value.param] = url.queryParams.getValue(key)
                        }
                    }
                }

                params
            } else
                null
    }

    companion object {

        operator fun invoke(endpoint: Endpoint) = invoke(endpoint.urlParts, endpoint.queryParameters)

        operator fun invoke(urlParts: List<EndpointUrlPart>, urlParams: Map<String, EndpointQueryParameter>): EndpointResolveTree {
            return build(listOf(), listOf(), urlParts, setOf(), setOf(), urlParams)
        }

        private fun build(
            fullPrefix: List<EndpointUrlPart>,
            currentPrefix: List<EndpointUrlPart>,
            rest: List<EndpointUrlPart>,
            taken: Set<String>,
            untaken: Set<String>,
            urlParams: Map<String, EndpointQueryParameter>
        ): EndpointResolveTree {
            if (rest.isEmpty()) {
                return if (currentPrefix.isEmpty())
                    Leaf(ResolveUrlPart.build(fullPrefix), taken, untaken, ResolveQueryParam.build(taken, untaken, urlParams))
                else
                    Multiple(
                        ResolveUrlPart.build(currentPrefix),
                        Leaf(ResolveUrlPart.build(fullPrefix + currentPrefix), taken, untaken, ResolveQueryParam.build(taken, untaken, urlParams))
                    )
            } else if (rest.none { it is EndpointPart.Optional }) {
                return Multiple(
                    ResolveUrlPart.build(currentPrefix + rest),
                    Leaf(
                        ResolveUrlPart.build(fullPrefix + currentPrefix + rest),
                        taken,
                        untaken,
                        ResolveQueryParam.build(taken, untaken, urlParams)
                    )
                )
            }

            var next = rest.first()

            if (next is EndpointPart.Optional && next.key in taken)
                next = next.part

            if (next is EndpointPart.Optional && next.key in untaken)
                return build(fullPrefix, currentPrefix, rest.drop(1), taken, untaken, urlParams)

            if (next is EndpointPart.Optional) {
                val takenPath = build(fullPrefix + currentPrefix, listOf(), listOf(next.part) + rest.drop(1), taken + next.key, untaken, urlParams)
                val untakenPath = build(fullPrefix + currentPrefix, listOf(), rest.drop(1), taken, untaken + setOf(next.key), urlParams)

                val options = mutableListOf(takenPath, untakenPath)
                while (options.any { it is Fork }) {
                    val i = options.indexOfFirst { it is Fork }
                    val fork = options.removeAt(i) as Fork
                    options.addAll(i, fork.options)
                }

                return if (currentPrefix.isEmpty())
                    Fork(options.toSet())
                else
                    Multiple(ResolveUrlPart.build(currentPrefix), Fork(options.toSet()))
            } else {
                return build(fullPrefix, currentPrefix + next, rest.drop(1), taken, untaken, urlParams)
            }
        }

    }
}