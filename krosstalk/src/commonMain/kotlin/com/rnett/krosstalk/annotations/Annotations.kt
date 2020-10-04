package com.rnett.krosstalk.annotations

import com.rnett.krosstalk.Krosstalk
import kotlin.reflect.KClass

//TODO the ability to return something like KrosstalkResponse (but deserialized on Success) directly, including error codes, etc.  Use expect, typealias Unit on server side?  Still need to return something
//  maybe force a return type of generic KrosstalkResponse?  Server can return success or error (or just success), turn http status codes into error returns.  Success/CustomError/HttpError?
//TODO  similar, but wrap server side in try/catch, send any exception's stack traces
//TODO option to not send instance/extension(?) receiver when it is an object
//TODO dummy/mock server/client

//@Target(AnnotationTarget.CLASS)
//@Retention(AnnotationRetention.BINARY)
//@MustBeDocumented
//annotation class KrosstalkHost

/**
 * Makes a method a krosstalk method.   Should only be on the `expect` declaration.
 *
 * By default, the endpoint will be "/krosstalk/$methodName" although the prefix can be overridden in Krosstalk.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
annotation class KrosstalkMethod(val klass: KClass<out Krosstalk>)


/**
 * Specifies an endpoint for the krosstalk method to use.
 * Can include arguments in the endpoint using `"{parameterName}"`. This will be evaluated with `toString()`.
 * These path arguments won't be used on the server side for evaluation (as everything is put in the request body),
 * but can be used to set endpoints based on arguments.
 * If you want to include non-trivial functions of the arguments in the endpoint, include them as default arguments and use the arguments.
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
annotation class KrosstalkEndpoint(val endpoint: String, val httpMethod: String = "POST")


//TODO error when same scope is in Required and Optional
/**
 * The listed scopes will be attached to this method's handler on the server, and applied to the request.  If they are not active, the request will not be sent and will error.
 * Should be on the `expect` declaration with [KrosstalkMethod].
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
annotation class RequiredScopes(vararg val scopes: String)

/**
 * The listed scopes will be attached to this method's handler on the server, and applied to the request if active.  If they are not active, the request will still be sent.
 * Should be on the `expect` declaration with [KrosstalkMethod].
 *
 * Note that these scopes will still always be applied to the server handler.  The optional vs required only applies to the client.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
annotation class OptionalScopes(vararg val scopes: String)

/**
 * Return null when the listed HTTP response codes are encountered.
 * For example, turning a "404: Item not found" into a null for Map.get like behavior.
 * Note that using this with codes like 404 or 500 can make debugging connection issues much harder.
 *
 * The return type must be nullable.  Only affects the client.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
annotation class NullOn(vararg val responseCodes: Int)

/**
 * Don't include arguments that are part of the [KrosstalkEndpoint] endpoint in the body.
 * **Only usable on client-only methods.**
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
annotation class MinimizeBody

/**
 * Don't include arguments that are part of the [KrosstalkEndpoint] endpoint in the body, and error if all arguments aren't in the endpoint.
 * **Only usable on client-only methods.**
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
annotation class EmptyBody