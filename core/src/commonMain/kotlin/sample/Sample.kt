package com.rnett.krosstalk.sample

import com.rnett.krosstalk.Krosstalk
import com.rnett.krosstalk.client.KrosstalkClient
import com.rnett.krosstalk.client.KrosstalkClientSerialization
import com.rnett.krosstalk.client.RequestMaker
import com.rnett.krosstalk.metadata.KrosstalkSpec
import com.rnett.krosstalk.metadata.MethodType
import com.rnett.krosstalk.metadata.ParameterType
import com.rnett.krosstalk.server.KrosstalkServer
import com.rnett.krosstalk.server.KrosstalkServerSerialization
import kotlin.reflect.typeOf

@Krosstalk
public interface SampleInterface {
    public suspend fun testFunc(a: Int, b: Int): String

    public companion object
}

private val spec = KrosstalkSpec<SampleInterface>(
    "com.rnett.krosstalk.sample.SampleInterface",
    "SampleInterface",
    mapOf(
        "testFunc" to MethodType(
            mapOf(
                "a" to ParameterType(typeOf<Int>()),
                "b" to ParameterType(typeOf<Int>())
            ),
            typeOf<String>()
        )
    )
)

public val SampleInterface.Companion.SPEC: KrosstalkSpec<SampleInterface> get() = spec

public abstract class Server(serialization: KrosstalkServerSerialization) : SampleInterface,
    KrosstalkServer<SampleInterface>(serialization) {

    override val spec: KrosstalkSpec<SampleInterface>
        get() = SampleInterface.SPEC

    override suspend fun invoke(methodName: String, arguments: Map<String, Any?>): Any? {
        if (methodName == "testFunc") {
            return testFunc(
                arguments["a"] as Int,
                arguments["b"] as Int
            )
        }

        throw IllegalStateException("Method $methodName not handled")
    }
}

public class ServerImpl(serialization: KrosstalkServerSerialization) : Server(serialization) {
    override suspend fun testFunc(a: Int, b: Int): String {
        return (a + b).toString()
    }
}

public class Client(
    baseUrl: String,
    requestMaker: RequestMaker,
    serialization: KrosstalkClientSerialization
) : KrosstalkClient<SampleInterface>(baseUrl, requestMaker, serialization), SampleInterface {
    override suspend fun testFunc(a: Int, b: Int): String {
        val argValues = mapOf(
            "a" to a,
            "b" to b
        )
        return invoke("testFunc", argValues) as String
    }

    override val spec: KrosstalkSpec<SampleInterface>
        get() = SampleInterface.SPEC

}