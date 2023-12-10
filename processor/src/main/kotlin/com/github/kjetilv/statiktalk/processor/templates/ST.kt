package com.github.kjetilv.statiktalk.processor.templates

import com.github.kjetilv.statiktalk.processor.KMessage
import com.github.kjetilv.statiktalk.processor.KParam
import com.github.kjetilv.statiktalk.processor.KService
import org.stringtemplate.v4.ST
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.max
import kotlin.math.roundToInt


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
    }.trim().let(::adorn).let { bg(service, it) }

private fun bg(service: KService, code: String) =
    code.split("\n").let { lines ->
        lines.indexOfFirst { line -> line.startsWith("@file") }
            .let { header ->

                val rightMargin = lines.map { it.indexOf(" */ // DO NOT TOUCH") }.first { it > 0 }
                val leftMargin = PRE.length

                val textWidth = rightMargin - leftMargin
                val textHeight = lines.size - header - 1

                val textShift = (textWidth * 0.4).roundToInt()
                val lineHeader = PRE.length + textShift

                val set: (Int, Int) -> Boolean = service.imgFun(textHeight, textWidth, textShift)

                lines.mapIndexed { textY, line ->
                    line.toCharArray().mapIndexed { textX, c ->
                        if (c == '`' &&
                            textX in lineHeader..<(leftMargin + textWidth) &&
                            textY in header..<(header + textHeight)
                        )
                            if (set(textY - header, textX - lineHeader))
                                'x'
                            else
                                '.'
                        else
                            c
                    }.joinToString("")
                }.joinToString("\n")
            }
    }

private fun KService.imgFun(
    textHeight: Int,
    textWidth: Int,
    textShift: Int
): (Int, Int) -> Boolean {
    val imgWidth = textHeight
    val imgHeight = textWidth - textShift

    val img = BufferedImage(imgWidth * 2, imgHeight, BufferedImage.TYPE_INT_RGB)
    val graphics = img.graphics as Graphics2D
    graphics.font = Font("Monospaced", Font.ITALIC, (imgHeight * 0.8f).roundToInt())
    graphics.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING,
        RenderingHints.VALUE_TEXT_ANTIALIAS_ON
    )
    graphics.drawString(name, (imgWidth * 0.2).roundToInt(), (imgHeight * 0.8).roundToInt())
    graphics.dispose()
    img.flush()

    return { x: Int, y: Int ->
        if (x >= imgWidth || y >= imgHeight) {
            throw IllegalArgumentException("$x >= $imgWidth || $y >= $imgHeight")
        }
        img.getRGB(x * 2, imgHeight - 1 - y) != -16777216
    }
}

private fun adorn(code: String) =
    code.split("\n")
        .map { it.trimEnd() }
        .let { lines ->
            lines.indexOfFirst { line -> line.startsWith("@file") }
                .let { header ->
                    lines.maxOf { it.length }
                        .let { len -> (len * 1.1).toInt() }
                        .let { len ->
                            lines.neighborhoods(1)
                                .mapIndexed { i, group -> i to group }
                                .map { (index, group) ->
                                    group.let { (line, neighbors) ->
                                        suffixed(index, line, neighbors, len, header)
                                    }
                                }
                                .framed()
                        }
                        .joinToString("\n")
                }
        }

private fun <E> List<E>.neighborhoods(neighbors: Int): List<Pair<E, List<E>>> =
    mapIndexed { i, line ->
        line to subList(maxOf(0, i - neighbors), minOf(size, i + neighbors))
    }

private const val STATICTALK = "statictalk "

private const val PRE = "/* $STATICTALK */ "

private const val POST = " // DO NOT TOUCH"

private fun suffixed(index: Int, line: String, neighbors: List<String>, maxLength: Int, header: Int) =
    (maxLength - line.length).let { buffer ->
        max(0, line.takeWhile(Char::isWhitespace).length).let { preamble ->
            (header > index).let { inHeader ->
                shuffled(index, STATICTALK, preamble, inHeader).let { shuffled ->
                    (if (inHeader) line else line.substring(preamble)).let { remainingLine ->
                        PRE.replace(STATICTALK, shuffled) + remainingLine + emptySpace(
                            line,
                            neighbors,
                            buffer,
                            inHeader
                        ) + POST
                    }
                }
            }
        }
    }

private fun emptySpace(
    line: String,
    neighbors: List<String>,
    buffer: Int,
    inHeader: Boolean
) =
    if (inHeader) " ".repeat(buffer)
    else line.endsWith(" ").let { spaced ->
        (if (spaced) 6 else 7).let { preamble ->
            if (buffer in 0..preamble)
                " ".repeat(buffer)
            else {
                val shift = maxOf(0, avgLength(neighbors) - line.length)
                val commentLength = buffer - preamble
                (if (spaced) "" else " ") + " ".repeat(shift) + "/* ${"`".repeat(commentLength - shift)} */"
            }
        }
    }

private fun avgLength(neighbors: List<String>): Int = neighbors.map(String::length).average().toInt()

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
