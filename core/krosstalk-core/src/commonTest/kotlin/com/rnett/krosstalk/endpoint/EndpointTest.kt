package com.rnett.krosstalk.endpoint

import kotlin.test.Test
import kotlin.test.assertEquals

class EndpointTest {
    val template = "/method/test/{{id}}/{{?user}}/[?t1:t2/t3/{{?t4}}]?test={test3}&[?test2:t=all&t1=2]"
    val endpoint = Endpoint(template, "method", "")

    @Test
    fun testParse() {
        assertEquals(EndpointPart.Static("method"), endpoint.urlParts[0])
        assertEquals(EndpointPart.Static("test"), endpoint.urlParts[1])
        assertEquals(EndpointPart.Static("id"), endpoint.urlParts[2])
        assertEquals(EndpointPart.Parameter("id"), endpoint.urlParts[3])
        assertEquals(EndpointPart.Optional("user", EndpointPart.Static("user")), endpoint.urlParts[4])
        assertEquals(EndpointPart.Optional("user", EndpointPart.Parameter("user")), endpoint.urlParts[5])
        assertEquals(EndpointPart.Optional("t1", EndpointPart.Static("t2")), endpoint.urlParts[6])
        assertEquals(EndpointPart.Optional("t1", EndpointPart.Static("t3")), endpoint.urlParts[7])
        assertEquals(EndpointPart.Optional("t1", EndpointPart.Optional("t4", EndpointPart.Static("t4"))), endpoint.urlParts[8])
        assertEquals(EndpointPart.Optional("t1", EndpointPart.Optional("t4", EndpointPart.Parameter("t4"))), endpoint.urlParts[9])
        assertEquals(10, endpoint.urlParts.size)

        assertEquals(EndpointPart.Parameter("test3"), endpoint.queryParameters["test"])
        assertEquals(EndpointPart.Optional("test2", EndpointPart.Static("all")), endpoint.queryParameters["t"])
        assertEquals(EndpointPart.Optional("test2", EndpointPart.Static("2")), endpoint.queryParameters["t1"])
        assertEquals(3, endpoint.queryParameters.size)
    }

    @Test
    fun testResolveOptionals() {
        assertEquals(Endpoint("method/test/id/{id}?test={test3}", "", ""), endpoint.resolveOptionals(setOf("id", "test3")))
        assertEquals(Endpoint("method/test/id/{id}/user/{user}?test={test3}", "", ""), endpoint.resolveOptionals(setOf("id", "test3", "user")))
        assertEquals(Endpoint("method/test/id/{id}/t2/t3?test={test3}", "", ""), endpoint.resolveOptionals(setOf("id", "test3", "t1")))
        assertEquals(
            Endpoint("method/test/id/{id}/t2/t3?test={test3}&t=all&t1=2", "", ""),
            endpoint.resolveOptionals(setOf("id", "test3", "t1", "test2"))
        )
    }

    @Test
    fun testFill() {
        val arguments1 = mapOf("id" to "2", "test3" to "22")
        assertEquals("method/test/id/2?test=22", endpoint.fillWithArgs("test", arguments1, arguments1.keys))

        val arguments2 = mapOf("id" to "2", "test3" to "22", "user" to "me", "t1" to "t")
        assertEquals("method/test/id/2/user/me/t2/t3?test=22", endpoint.fillWithArgs("test", arguments2, arguments2.keys))

        val arguments3 = mapOf("id" to "2", "test3" to "22", "user" to "me", "t1" to "t", "test2" to "t2")
        assertEquals("method/test/id/2/user/me/t2/t3?test=22&t=all&t1=2", endpoint.fillWithArgs("test", arguments3, arguments3.keys))
    }

    @Test
    fun testExtraction() {
        val resolver = endpoint.withStatic("method", "prefix").resolveTree
        assertEquals(mapOf("id" to "2", "test3" to "1"), resolver.resolve(UrlRequest("/method/test/id/2/?test=1")))
        assertEquals(null, resolver.resolve(UrlRequest("/method/test/id/2/t?test=1")))
        assertEquals(mapOf("id" to "2", "test3" to "1", "t4" to "t"), resolver.resolve(UrlRequest("/method/test/id/2/t2/t3/t4/t?test=1")))
        assertEquals(mapOf("id" to "2", "test3" to "1", "t4" to "t"), resolver.resolve(UrlRequest("/method/test/id/2/t2/t3/t4/t?test=1&e=extra")))
        assertEquals(null, resolver.resolve(UrlRequest("/method/test/id/2/t2/t3/t4/t/a/b/c?test=1")))
        assertEquals(mapOf("id" to "2", "test3" to "1", "user" to "me"), resolver.resolve(UrlRequest("/method/test/id/2/user/me?test=1")))
        assertEquals(mapOf("id" to "2", "test3" to "1", "user" to "me"), resolver.resolve(UrlRequest("/method/test/id/2/user/me?test=1&t=all&t1=2")))
        assertEquals(null, resolver.resolve(UrlRequest("/method/test/id/2/user/me?test=1&t=all&t1=3")))
        assertEquals(null, resolver.resolve(UrlRequest("/method/test/user/me?test=1")))
        assertEquals(null, resolver.resolve(UrlRequest("/method2/test/id/2/user/me?test=1")))
    }

    @Test
    fun testBasic() {
        val resolver = Endpoint.withoutStatic("/krosstalk/basic").resolveTree
        assertEquals(mapOf(), resolver.resolve(UrlRequest("/krosstalk/basic")))
    }

    @Test
    fun testTree() {
        val urlParts = listOf(
            EndpointPart.Static("1"),
            EndpointPart.Optional("a", EndpointPart.Optional("b", EndpointPart.Static("2"))),
            EndpointPart.Optional("b", EndpointPart.Static("3")),
            EndpointPart.Static("4"),
            EndpointPart.Static("5"),
            EndpointPart.Static("6"),
            EndpointPart.Optional("c", EndpointPart.Static("7")),
            EndpointPart.Optional("c", EndpointPart.Static("8"))
        )

        val urlParams = mapOf<String, EndpointQueryParameter>(
            "x" to EndpointPart.Optional("a", EndpointPart.Static("1")),
            "y" to EndpointPart.Optional("c", EndpointPart.Static("2")),
            "z" to EndpointPart.Static("3"),
        )

        val endpoint = Endpoint(EndpointRegion(urlParts), EndpointRegion(urlParams))
        val tree = EndpointResolveTree(endpoint)
        val treeOptions = tree.enumerate()
        val endpointOptions = endpoint.enumerateOptionals().values.map { ResolveEndpoint(it) }.toSet()
        assertEquals(endpointOptions, treeOptions)
    }
}