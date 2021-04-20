package com.rnett.krosstalk


/**
 * Marks an API as an internal Krosstalk API, that shouldn't be used.
 *
 * These APIs may change without notice, and generally do things that users shouldn't be doing.
 */
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn(message = "This API is for internal Krosstalk use.  Stability is not guaranteed and you probably shouldn't use it.")
public annotation class InternalKrosstalkApi

/**
 * Marks an API that is intended to be used with implementing new clients or servers, and shouldn't be used otherwise.
 *
 * New client or server implementations should opt in to this.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.TYPEALIAS)
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn(message = "This API is used when implementing clients or servers and should not be used outside of such.")
public annotation class KrosstalkPluginApi