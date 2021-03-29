package com.rnett.krosstalk.annotations

import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.KrosstalkResult
import com.rnett.krosstalk.defaultEndpoint
import com.rnett.krosstalk.defaultEndpointMethod
import com.rnett.krosstalk.exception
import com.rnett.krosstalk.exceptionMessage
import com.rnett.krosstalk.exceptionStacktrace
import com.rnett.krosstalk.serialization.SerializationHandler
import kotlin.reflect.KClass


//TODO option to not send instance/extension(?) receiver when it is an object

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

//TODO option to set content type
//TODO include param hash in default endpoint
//TODO update docs
//TODO optional handling.  Make explicit?  Currently nullables don't get sent if optional in url, defaults aren't required on server
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

//TODO respond with 500 code for server exceptions
//TODO option to only catch certain types of exceptions?  What's the point, can use
//TODO option to only do http errors, or only do exceptions (based on return type?) (should use separate result classes or sealed interfaces) (http error one should be usable wth CatchAsHttpError)
//TODO post 1.5: a version that uses kotlin.Result.  Would have to limit to http errors, can't serialize exceptions (test, can I have a custom serializable annotation?)

//TODO option (seperate annotation?) to convert some exceptions (by class) to HTTP error codes.  Useful with NullOn or this
//      want to do server side though, so things still match
//      big concern with NullOn is server and client stop matching, easy enough to use Result + wrappers where needed.
/**
 * Return a [KrosstalkResult], wrapping server exceptions or http errors.  Method return type must be [KrosstalkResult].
 * Server side function should return a [KrosstalkResult.Success].
 *
 * The server function will automatically be wrapped in a `try` block, converting thrown exceptions to
 * [KrosstalkResult.ServerException] (note that this applies when calling from server or client).  If
 * [printExceptionStackTraces] is `true`, the stack trace of any caught exceptions will be printed using [Throwable.printStackTrace].
 *
 * If [propagateServerExceptions] is `true`, the server implementation will re-throw or somehow log any server exceptions **after
 * the call completes** (so the client will receive a [KrosstalkResult.ServerException] result).
 * Exactly what is done depends on the server implementation used, but at a minimum it should show up in logs.
 *
 * [includeStacktrace] controls whether to include the stack trace of exceptions (via [Throwable.stackTraceToString]) in the
 * [KrosstalkResult.ServerException].  It may expose more information about the server than you want, so it is `false` by default.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@TopLevelOnly
annotation class ExplicitResult(
    val includeStacktrace: Boolean = false,
    val propagateServerExceptions: Boolean = false,
    val printExceptionStackTraces: Boolean = true,
)

//TODO option to log
/**
 * Only usable with [@ExplicitResult][ExplicitResult].  Converts any caught exceptions of type [exceptionClass] (or a subtype) to a
 * [KrosstalkResult.HttpError] response, with the given [responseCode] and [message].
 *
 * [exception], [exceptionMessage], and [exceptionStacktrace] can be used in [message], and will be replaced by
 * [Throwable.toString] (normally `"$class: $message"`), [Throwable.message] (`?: "N/A"`), and [Throwable.stackTraceToString], respectively.
 *
 * Note that this annotation may be used on the server side `actual` function, since this conversion is done entirely on the server side.
 *
 * Instance checking (i.e. `exceptionClass.isInstance(t)`) will be done in order, so put any high level [CatchAsHttpError]s last.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@Repeatable
annotation class CatchAsHttpError(
    val exceptionClass: KClass<out Throwable>,
    val responseCode: Int,
    val message: String = exceptionMessage,
)
