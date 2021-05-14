package com.rnett.krosstalk

import kotlin.jvm.JvmName

/**
 * A map of headers.  Case-insensitive, all header names are converted to lower case.
 *
 * Note that each header can have multiple values.
 *
 * @see MutableHeaders
 */
public open class Headers internal constructor(protected open val map: Map<String, List<String>>) {
    public fun toMap(): Map<String, List<String>> = map
    public fun toHeaders(): Headers = Headers(map.toMap())
    public fun toSingleValues(): List<Pair<String, String>> = map.toList().flatMap { (key, value) -> value.map { key to it } }

    public inline val keys: Set<String> get() = toMap().keys

    public operator fun get(key: String): List<String>? = map[key.lowercase()]
    public inline fun getOrElse(key: String, defaultValue: () -> List<String>): List<String> = this[key] ?: defaultValue()
    public inline fun getOrEmpty(key: String): List<String> = getOrElse(key) { emptyList() }

    public operator fun contains(key: String): Boolean = key.lowercase() in map

    public inline fun forEach(body: (String, List<String>) -> Unit): Unit = toMap().forEach { body(it.key, it.value) }
    public inline fun forEachPair(body: (String, String) -> Unit): Unit = toMap().forEach { (key, value) -> value.forEach { body(key, it) } }

    public inline fun anyOf(key: String, body: (String) -> Boolean): Boolean = getOrEmpty(key).any(body)
    public inline fun allOf(key: String, body: (String) -> Boolean): Boolean = getOrEmpty(key).all(body)

    public fun plus(key: String, value: String): Headers = this + (key to value)

    @JvmName("plusList")
    public fun plus(key: String, value: List<String>): Headers = this + (key to value)

    public operator fun plus(pair: Pair<String, String>): Headers {
        val (key, value) = pair
        val realKey = key.lowercase()

        return Headers(map + (realKey to getOrEmpty(realKey) + value))
    }

    @JvmName("plusList")
    public operator fun plus(pair: Pair<String, List<String>>): Headers {
        val (key, value) = pair
        val realKey = key.lowercase()

        return Headers(map + (realKey to getOrEmpty(realKey) + value))
    }

    public operator fun plus(headers: Headers): Headers {
        val newMap = map.toMutableMap()
        headers.forEach { key, list ->
            newMap[key] = newMap.getOrElse(key) { emptyList() } + list
        }
        return Headers(newMap)
    }

    @JvmName("plusList")
    public operator fun plus(headers: Map<String, List<String>>): Headers {
        return this + headers.toHeaders()
    }

    public operator fun plus(headers: Map<String, String>): Headers {
        return this + headers.toHeaders()
    }

    @JvmName("plusList")
    public operator fun plus(headers: Iterable<Pair<String, List<String>>>): Headers {
        return this + headers.toMap().toHeaders()
    }

    public operator fun plus(headers: Iterable<Pair<String, String>>): Headers {
        return this + headers.toMap().toHeaders()
    }

    public operator fun minus(key: String): Headers = Headers(map - key)
    public operator fun minus(keys: Iterable<String>): Headers = Headers(map - keys)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Headers) return false

        if (map != other.map) return false

        return true
    }

    override fun hashCode(): Int {
        return map.hashCode()
    }

    override fun toString(): String {
        return "Headers($map)"
    }
}


/**
 * A mutable map of headers.  Case-insensitive, all header names are converted to lower case.
 *
 * Note that each header can have multiple values, and as such [set] and [plusAssign] methods add
 * to the header list instead of setting it.
 *
 * @see Headers
 */
public class MutableHeaders internal constructor(override val map: MutableMap<String, List<String>>) : Headers(map) {
    public operator fun set(key: String, value: String) {
        val realKey = key.lowercase()
        map[realKey] = map.getOrElse(realKey) { emptyList() } + value
    }

    public operator fun set(key: String, value: List<String>) {
        map[key.lowercase()] = map.getOrElse(key) { emptyList() } + value
    }

    public fun clear() {
        map.clear()
    }

    public fun remove(key: String) {
        map.remove(key.lowercase())
    }

    public fun removeAll(keys: Iterable<String>) {
        keys.forEach { remove(it) }
    }

    public operator fun plusAssign(pair: Pair<String, String>) {
        this[pair.first] = pair.second
    }

    @JvmName("plusAssignList")
    public operator fun plusAssign(pair: Pair<String, List<String>>) {
        this[pair.first] = pair.second
    }

    public operator fun plusAssign(headers: Headers) {
        headers.forEach { key, list -> this[key] = list }
    }

    @JvmName("plusAssignList")
    public operator fun plusAssign(headers: Map<String, List<String>>) {
        this += headers.toHeaders()
    }

    public operator fun plusAssign(headers: Map<String, String>) {
        this += headers.toHeaders()
    }

    @JvmName("plusAssignList")
    public operator fun plusAssign(headers: Iterable<Pair<String, List<String>>>) {
        headers.forEach { (key, value) -> this[key] = value }
    }

    public operator fun plusAssign(headers: Iterable<Pair<String, String>>) {
        headers.forEach { (key, value) -> this[key] = value }
    }

    public operator fun minusAssign(key: String) {
        remove(key)
    }

    public operator fun minusAssign(keys: Iterable<String>) {
        removeAll(keys)
    }

    override fun toString(): String {
        return "MutableHeaders($map)"
    }
}

public fun headersOf(): Headers = Headers(emptyMap())
public fun headersOf(key: String, value: String): Headers = mapOf(key to value).toHeaders()
public fun headersOf(key: String, value: List<String>): Headers = mapOf(key to value).toHeaders()
public fun headersOf(key: String, vararg values: String): Headers = mapOf(key to values.toList()).toHeaders()
public fun headersOf(vararg pairs: Pair<String, String>): Headers = mapOf(*pairs).toHeaders()

@JvmName("headersOfList")
public fun headersOf(vararg pairs: Pair<String, List<String>>): Headers = mapOf(*pairs).toHeaders()

public fun mutableHeadersOf(): MutableHeaders = MutableHeaders(mutableMapOf())
public fun mutableHeadersOf(key: String, value: String): MutableHeaders = mapOf(key to value).toHeaders().toMutableHeaders()
public fun mutableHeadersOf(key: String, value: List<String>): MutableHeaders = mapOf(key to value).toHeaders().toMutableHeaders()
public fun mutableHeadersOf(key: String, vararg values: String): MutableHeaders = mapOf(key to values.toList()).toHeaders().toMutableHeaders()
public fun mutableHeadersOf(vararg pairs: Pair<String, String>): MutableHeaders = mapOf(*pairs).toHeaders().toMutableHeaders()

@JvmName("mutableHeadersOfList")
public fun mutableHeadersOf(vararg pairs: Pair<String, List<String>>): MutableHeaders = mapOf(*pairs).toHeaders().toMutableHeaders()

public inline fun Headers?.orEmpty(): Headers = this ?: headersOf()

public inline fun buildHeaders(block: MutableHeaders.() -> Unit): Headers {
    return mutableHeadersOf().apply(block)
}

public inline fun Headers.addHeaders(block: MutableHeaders.() -> Unit): Headers {
    return this + mutableHeadersOf().apply(block)
}

public fun Headers.toMutableHeaders(): MutableHeaders = MutableHeaders(toMap().toMutableMap())

@JvmName("listToHeaders")
public fun Map<String, List<String>>.toHeaders(): Headers = Headers(this.mapKeys { it.key.lowercase() })
public fun Map<String, String>.toHeaders(): Headers = Headers(this.mapKeys { it.key.lowercase() }.mapValues { listOf(it.value) })