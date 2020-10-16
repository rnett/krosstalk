package com.rnett.krosstalk

@PublishedApi
internal val valueRegex = Regex("(\\{[^}]+?\\})")

//TODO more complex endpoint stuff.  optional arguments, optional keys (i.e. "/id/{id}" onlt if id is != null), etc.
//  needs support for server side default args?  (i.e. don't send and use default) I think this is actually supported in `call`

fun getUrlPath(url: String): String {
    // something like test/url/part or /test/part/2
    return if (url.substringBefore("/", "").matches(Regex("[a-z]*", RegexOption.IGNORE_CASE)))
        url
    else
        url.substringAfter("://").substringAfter("/", "")
}

data class Endpoint(
    val urlParts: List<EndpointPart>,
    val queryParameters: Map<String, EndpointPart>
) {
    companion object {
        operator fun invoke(template: String): Endpoint {

            val urlParts = template
                .substringBefore("?")
                .trim('/')
                .split('/')
                .map {
                    if (valueRegex.matchEntire(it) != null)
                        EndpointPart.Parameter(it.substring(1, it.length - 1))
                    else
                        EndpointPart.Static(it)
                }

            val queryParameters = template
                .substringAfter("?", "")
                .ifBlank { null }
                ?.split("&")
                ?.associate {
                    it.substringBefore("=") to EndpointPart(it.substringAfter("=", ""))
                } ?: emptyMap()

            return Endpoint(urlParts, queryParameters)
        }
    }

    fun extractParameters(url: String): Map<String, String> {
        val endpoint = getUrlPath(url)
        val urlParts = endpoint
            .substringBefore("?")
            .trim('/')
            .split('/')

        val queryParameters = endpoint
            .substringAfter("?", "")
            .split("&")
            .associate {
                it.substringBefore("=") to it.substringAfter("=", "")
            }

        val urlParams = urlParts.zip(this.urlParts).mapNotNull { (value, param) ->
            if (param is EndpointPart.Parameter)
                param.key to value
            else
                null
        }.toMap()

        val queryParams = queryParameters.entries.mapNotNull { (key, value) ->
            this.queryParameters[key]?.let {
                if (it is EndpointPart.Parameter)
                    it.key to value
                else
                    null
            }
        }.toMap()

        return urlParams + queryParams
    }

    fun toStringForParts(partToString: (EndpointPart) -> String) = buildString {
        append(urlParts.joinToString("/") { partToString(it) })

        if (queryParameters.isNotEmpty()) {
            append("?")
            append(
                queryParameters.entries.joinToString("&") { (key, value) ->
                    "$key=${partToString(value)}"
                }
            )
        }
    }

    fun toString(wrapParameter: (String) -> String) = toStringForParts { it.toString(wrapParameter) }

    fun toString(start: String, end: String) = toString { "$start$it$end" }
    override fun toString(): String = toString("{", "}")

    inline fun fill(crossinline fillParameter: (key: String) -> String) = toStringForParts {
        it.fill(fillParameter)
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
    inline fun fillWithStaticAndAdjustParameters(
        methodName: String,
        prefix: String,
        crossinline wrapParameter: (String) -> String = { "{$it}" }
    ) = fill {
        when (it) {
            methodNameKey -> methodName
            prefixKey -> prefix
            else -> wrapParameter(it)
        }
    }

    /**
     * Substitute static variables (method name and krosstalk prefix) into the endpoint template.
     *
     * @see [fillWithStaticAndAdjustParameters]
     */
    fun fillWithStatic(methodName: String, prefix: String) =
        fillWithStaticAndAdjustParameters(methodName, prefix)


    /**
     * Substitute argument values into an endpoint template.  Requires all keys in the template to be used.
     *
     * @see [fill]
     *
     * @return The filled endpoint, and the parameters used to fill it (not including the method name or prefix).
     */
    fun fillWithArgs(
        methodName: String,
        prefix: String,
        arguments: Map<String, String>
    ): String = fill {
        when (it) {
            methodNameKey -> methodName
            prefixKey -> prefix
            in arguments -> arguments.getValue(it)
            else -> throw KrosstalkException.EndpointUnknownArgument(methodName, this, it, arguments.keys)
        }
    }

    fun usedParameters(excludeStatic: Boolean = true) =
        (urlParts + queryParameters.values).filterIsInstance<EndpointPart.Parameter>().map { it.key }.let {
            if (excludeStatic)
                it.filter { it != methodNameKey && it != prefixKey }
            else
                it
        }.toSet()

    val usedArguments by lazy { usedParameters() }
}


sealed class EndpointPart {
    companion object {
        operator fun invoke(text: String): EndpointPart =
            if (valueRegex.matchEntire(text) != null)
                Parameter(text.substring(1, text.length - 1))
            else
                Static(text)
    }

    abstract fun toString(wrapParameter: (String) -> String): String

    inline fun fill(fillParameter: (key: String) -> String) = when (this) {
        is Static -> part
        is Parameter -> fillParameter(key)
    }

    data class Parameter(val key: String) : EndpointPart() {
        val isMethodName = key == methodNameKey
        val isPrefix = key == prefixKey
        val isExtensionReceiver = key == extensionParameterKey
        val isInstanceReceiver = key == instanceParameterKey

        override fun toString(): String = toString { "{$it}" }
        override fun toString(wrapParameter: (String) -> String): String = wrapParameter(key)
    }

    data class Static(val part: String) : EndpointPart() {
        override fun toString(wrapParameter: (String) -> String): String = toString()
        override fun toString(): String = part
    }
}