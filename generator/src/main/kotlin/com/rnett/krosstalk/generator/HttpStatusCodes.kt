package com.rnett.krosstalk.generator

import org.jsoup.Jsoup
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.writeText

fun generateStatusCodeMap(indent: String = "    "): String = buildString {
    val document = Jsoup.connect("https://developer.mozilla.org/en-US/docs/Web/HTTP/Status").get()
    val codes = document.select("a > code").map { it.text() }

    val map = codes.mapNotNull { str ->
        val num = str.substringBefore(' ', "").ifBlank { null }?.toIntOrNull()
        num?.let { it to str.substringAfter(' ') }
    }.sortedBy { it.first }.toMap()

    append("mapOf<Int, String>(\n")
    map.forEach {
        append("$indent${it.key} to \"${it.value}\",\n")
    }
    append(")")
}

@OptIn(ExperimentalPathApi::class)
fun main(args: Array<String>) {
    if (args.isEmpty()) {
        System.err.println("1st argument must be file to output to.")
    }

    val packageStr = args.getOrNull(1)

    val file = Path(args[0])

    file.writeText(buildString {

        if (packageStr != null)
            append("package $packageStr\n\n")

        append("// THIS FILE IS GENERATED, DO NOT EDIT\n\n")

        append("val httpStatusCodes = ${generateStatusCodeMap()}")
    })
}