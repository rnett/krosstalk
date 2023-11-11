package com.rnett.krosstalk.server

public class KrosstalkMethodNotFoundException(className: String, methodName: String) :
    RuntimeException("Krosstalk method $methodName does not exist on class $className") {
}