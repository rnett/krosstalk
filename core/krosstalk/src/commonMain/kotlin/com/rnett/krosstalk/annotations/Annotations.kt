package com.rnett.krosstalk.annotations

import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.KrosstalkResult
import com.rnett.krosstalk.defaultEndpoint
import com.rnett.krosstalk.defaultEndpointMethod
import com.rnett.krosstalk.serialization.SerializationHandler
import kotlin.reflect.KClass

//TODO !! enforce empty body when get is used as method in endpoint.

//TODO option to not send instance/extension(?) receiver when it is an object
//TODO dummy/mock server/client

// meta annotations
@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
internal annotation class ClientOnly

@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
internal annotation class TopLevelOnly

/**
 * Makes a method a krosstalk method.   Should only be on the `expect` declaration.
 *
 * By default, the endpoint will be "/krosstalk/$methodName" although the prefix can be overridden in Krosstalk.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@TopLevelOnly
annotation class KrosstalkMethod(val klass: KClass<out Krosstalk>)

//TODO include param hash in default endpoint
/*
TODO >
    see to-do in Endpoint.kt
    I want to include more advanced syntax.
    key-value pairs, i.e. {{id}} -> /id/{id} or id={id} depending on location
    optional arguments.  maybe only key value pairs?  or something like [?id:...]
        how to determine when not sent?  can't really detect default use from method call.  nulls?  another boolean arg?


    create route dsl and use ar argument to `krosstalkCall()`?  could pass to the register without much work

 */
/**
 * Specifies an endpoint for the krosstalk method to use.
 * Should be a http-formatted string of the pathname and query string, i.e. `"/items/?id={id}"`.
 * Can include arguments in the endpoint using `"{parameterName}"`.
 * This will be evaluated by serializing the argument using the Krosstalk's serialization.
 * I highly recommend you use a string serialization method ([SerializationHandler] with [String]) when using this.
 * Unless you specify [MinimizeBody] or [EmptyBody] the arguments used in the endpoint will still be passed in the body.
 * If you want to include non-trivial functions of the arguments in the endpoint, include them in the function as default arguments and use those.
 *
 * For instance and extension receivers, use `"{\$instance}"` and `"{\$extension}"`, respectively.
 * To include the name of the method, use `"{\$name}"`.
 * To include the preset set in the Krosstalk object, use `"{\$prefix}"` (it is not included automatically if [KrosstalkEndpoint] is present).
 *
 * The default endpoint is thus `"{\$prefix}/{\$name}"`.
 *
 * **Note:** don't include the `'\'` in the literals, it is there to ensure that the `'$'` doesn't get counted as a string template.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@TopLevelOnly
annotation class KrosstalkEndpoint(val endpoint: String = defaultEndpoint, val httpMethod: String = defaultEndpointMethod)

/**
 * Return null when the listed HTTP response codes are encountered.
 * For example, turning a "404: Item not found" into a null for Map.get like behavior.
 * Note that using this with codes like 404 or 500 can make debugging connection issues much harder.
 *
 * The return type must be nullable.  Only affects the client.
 * TODO get rid of in favor of better KrosstalkResult API
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@TopLevelOnly
annotation class NullOn(vararg val responseCodes: Int)

/**
 * Don't include arguments that are part of the [KrosstalkEndpoint] endpoint in the body.
 * Those arguments will be passed in the endpoint.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@TopLevelOnly
annotation class MinimizeBody

/**
 * Don't include arguments that are part of the [KrosstalkEndpoint] endpoint in the body, and error if all arguments aren't in the endpoint.
 * Those arguments will be passed in the endpoint.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@TopLevelOnly
annotation class EmptyBody

//TODO option to re-throw caught exceptions on the server
//TODO option to only catch certain types of exceptions?  What's the point
//TODO option to only do http errors, or only do exceptions
//TODO post 1.5: a version that uses kotlin.Result.  Would have to limit to http errors, can't serialize exceptions (test)
/**
 * Return [KrosstalkResult].  Method return type must be [KrosstalkResult].
 * Server side function should return a [KrosstalkResult.Success].
 * [KrosstalkResult.HttpError] and [KrosstalkResult.Exception] will be used depending on the call result.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@TopLevelOnly
annotation class ExplicitResult(val includeStacktrace: Boolean = false)
