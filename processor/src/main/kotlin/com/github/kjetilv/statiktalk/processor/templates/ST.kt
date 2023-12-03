package com.github.kjetilv.statiktalk.processor.templates

import com.github.kjetilv.statiktalk.processor.KMessage
import com.github.kjetilv.statiktalk.processor.KParam
import com.github.kjetilv.statiktalk.processor.KService
import org.stringtemplate.v4.ST
import java.math.BigDecimal
import java.math.BigInteger

internal fun String.source(service: KService, messages: List<KMessage>) =
    try {
        ST(this, '《', '》').apply {
            add("s", service)
            add("ms", messages)
            add("ps", combinedParams(messages))
            add("imports", imports(messages))
        }.render()
    } catch (e: Exception) {
        throw IllegalStateException("Failed to render $service, ${messages.size} messages", e)
    }.trim().let { adorn(it) }

private fun adorn(code: String) =
    code.split("\n").let { lines ->
        lines.maxOf { it.length }.let { maxLength ->
            lines.map { line -> suffixed(line, maxLength) }.framed(maxLength).joinToString("\n")
        }
    }

private const val PRE = "/* statictalk */ "

private const val POST = " // DO NOT TOUCH"

private fun List<String>.framed(maxLength: Int) =
    maxOf(0, maxLength + PRE.length + POST.length - 6).let { length ->
        ("/* " + "-".repeat(length) + " */").let { frame ->
            listOf(frame) + this + listOf(frame)
        }
    }

private fun suffixed(line: String, maxLength: Int) =
    (maxLength - line.length).let { buffer ->
        PRE + line + " ".repeat(buffer) + POST
    }

private fun imports(messages: List<KMessage>) =
    messages.flatMap { message -> message.keys.map { key -> key.type } }
        .distinct()
        .let { types -> explicit(types) + implicit(types, BigDecimal::class.java, BigInteger::class.java) }

private fun explicit(types: List<String>) =
    types.filter { type -> type.startsWith("java") || type.startsWith("kotlin") }

private fun implicit(types: List<String>, vararg implicits: Class<*>) =
    implicits.flatMap { implicit ->
        if (types.contains(implicit.simpleName)) listOf(implicit.name) else emptyList()
    }

private fun combinedParams(messages: List<KMessage>) =
    messages.flatMap(KMessage::keys).distinctBy(KParam::name)
        .map { (name, type) -> KParam(name, type) }
