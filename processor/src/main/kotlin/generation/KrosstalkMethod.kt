package com.rnett.krosstalk.processor.generation

import com.google.devtools.ksp.symbol.KSTypeReference

data class KrosstalkMethod(
    val name: String,
    val parmeters: Map<String, KSTypeReference>,
    val returnType: KSTypeReference
) {
}