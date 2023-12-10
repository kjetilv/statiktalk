package com.github.kjetilv.statiktalk.processor.templates

import com.github.kjetilv.statiktalk.processor.KMessage
import com.github.kjetilv.statiktalk.processor.KParam
import com.github.kjetilv.statiktalk.processor.KService
import org.stringtemplate.v4.ST
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.max

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
    code.split("\n")
        .map { it.trimEnd() }
        .let { lines ->
            lines.indexOfFirst { line -> line.startsWith("@file") }
                .let { header ->
                    lines.maxOf { it.length }
                        .let { len -> (len * 1.1).toInt() }
                        .let { len ->
                            lines.mapIndexed { i, line ->
                                suffixed(i, line, len, header)
                            }
                                .framed()
                        }
                        .joinToString("\n")

                }
        }

private const val STATICTALK = "statictalk "

private const val PRE = "/* $STATICTALK */ "

private const val POST = " // DO NOT TOUCH"

private fun suffixed(index: Int, line: String, maxLength: Int, header: Int) =
    (maxLength - line.length).let { buffer ->
        max(0, line.takeWhile(Char::isWhitespace).length).let { preamble ->
            (header > index).let { inHeader ->
                shuffled(index, STATICTALK, preamble, inHeader).let { shuffled ->
                    (if (inHeader) line else line.substring(preamble)).let { remainingLine ->
                        PRE.replace(STATICTALK, shuffled) + remainingLine + emptySpace(line, buffer, inHeader) + POST
                    }
                }
            }
        }
    }

private fun emptySpace(
    line: String,
    buffer: Int,
    inHeader: Boolean
) =
    if (inHeader) " ".repeat(buffer)
    else line.endsWith(" ").let { spaced ->
        (if (spaced) 6 else 7).let { preamble ->
            if (buffer in 0..preamble) " ".repeat(buffer)
            else (if (spaced) "" else " ") + "/* ${"`".repeat(buffer - preamble)} */"
        }
    }

private fun List<String>.framed() =
    listOf("// @formatter:off") + this + listOf("// @formatter:on")

fun shuffled(index: Int, str: String, preamble: Int, inHeader: Boolean) =
    (index % str.length).let { pos ->
        str.substring(pos) + str.substring(0, pos)
    }.let { base ->
        if (inHeader) base else base + repeated(base, preamble)
    }

private fun repeated(base: String, preamble: Int): String {
    val len = base.length
    return base.substring(0, minOf(preamble, len)) + (if (preamble >len) {
        repeated(base, preamble - len)
    } else {
        ""
    })
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
