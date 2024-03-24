package com.github.kjetilv.statiktalk.processor.templates

import com.github.kjetilv.statiktalk.processor.KMessage
import com.github.kjetilv.statiktalk.processor.KParam
import com.github.kjetilv.statiktalk.processor.KService
import org.stringtemplate.v4.ST
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.math.BigDecimal
import java.math.BigInteger
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.roundToInt

internal fun String.source(
    type: String,
    service: KService,
    messages: List<KMessage>
) =
    try {
        ST(this, '《', '》').apply {
            add("s", service)
            add("ms", messages)
            add("ps", combinedParams(messages))
            add("imports", imports(messages))
        }.render()
    } catch (e: Exception) {
        throw IllegalStateException("Failed to render $service, ${messages.size} messages", e)
    }.trim().let(::adorn).let { bg(type, service, it) }

private const val MAIN = "#"

private const val DROP = "#"

private const val BACK = "."

private const val ZAXS = "`"

private fun bg(type: String, service: KService, code: String) =
    code.split("\n").let { lines ->
        lines.indexOfFirst(::codeStart).let { offset ->

            val rMargin = lines.map { it.indexOf(" */ // DO NOT TOUCH") }.first { it > 0 }
            val lMargin = PRE.length

            val tw = rMargin - lMargin
            val th = lines.size - offset - 1

            val shift = (tw * .4).roundToInt()
            val indent = PRE.length + shift

            infix fun Int.xy(y: Int) =
                service.imgFun(type, th, tw, shift, 4).let { it(this, y) }

            lines.mapIndexed { ty, line ->
                line.toCharArray().mapIndexed { tx, c ->
                    if (
                        "$c" == ZAXS &&
                        tx in indent..<(lMargin + tw) &&
                        ty in offset..<(offset + th)
                    )
                        if (
                            (ty - offset) xy (tx - indent)
                        )
                            if (
                                (ty - offset) xy (tx - indent - 1) && (ty - offset) xy (tx - indent + 1)
                            )
                                MAIN
                            else
                                DROP
                        else
                            BACK
                    else
                        c
                }.joinToString("")
            }.joinToString("\n")
        }
    }

private fun KService.imgFun(
    type: String,
    textHeight: Int,
    textWidth: Int,
    textShift: Int,
    squeeze: Int = 1
): (Int, Int) -> Boolean {
    val imgWidth = textHeight
    val imgHeight = textWidth - textShift

    val img = BufferedImage(imgWidth * squeeze, imgHeight, BufferedImage.TYPE_INT_RGB)
    val g2d = img.graphics as Graphics2D
    g2d.font = properHeightFont(g2d, name, imgHeight)
    g2d.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING,
        RenderingHints.VALUE_TEXT_ANTIALIAS_ON
    )
    g2d.drawString(name.uppercase(), 1, imgHeight - 2)
    g2d.dispose()
    img.flush()

    ImageIO.write(img, "png", File("${System.getProperty("user.home")}/Downloads/$name-$type.png"))

    return { x: Int, y: Int ->
        if (x >= imgWidth || y >= imgHeight) {
//            throw IllegalArgumentException("$x >= $imgWidth || $y >= $imgHeight: $img")
            false
        } else {
            try {
                img.getRGB(x * squeeze, imgHeight - y) != -16777216
            } catch (e: Exception) {
//                throw IllegalArgumentException("$x >= $imgWidth || $y >= $imgHeight: $img", e)
                false
            }
        }
    }
}

private fun properHeightFont(g2d: Graphics2D, name: String, imgHeight: Int) =
    generateSequence(20) { size ->
        size + 1
    }.map { size ->
        Font(Font.MONOSPACED, Font.PLAIN, size)
    }.takeWhile { font ->
        g2d.getFontMetrics(font).height < (imgHeight * 1.5).toInt()
    }.last()

private fun adorn(code: String): String =
    code.split("\n")
        .map { it.trimEnd() }
        .let { lines ->
            lines.indexOfFirst(::codeStart).let { header ->
                lines.maxOf { it.length }
                    .let { len ->
                        (len * 1.1).toInt()
                    }
                    .let { len ->
                        lines.neighborhoods(0)
                            .mapIndexed { i, group ->
                                i to group
                            }
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

private fun codeStart(line: String) = line.contains("@file:Suppress(")

private fun suffixed(index: Int, line: String, neighbors: List<String>, maxLength: Int, header: Int) =
    (maxLength - line.length).let { buffer ->
        max(0, line.takeWhile(Char::isWhitespace).length).let { preamble ->
            (header > index).let { inHeader ->
                shuffled(index, STATICTALK, preamble, inHeader).let { shuffled ->
                    "${
                        PRE.replace(STATICTALK, shuffled)
                    }${
                        if (inHeader) line else line.substring(preamble)
                    }${
                        emptySpace(
                            line, neighbors, buffer, inHeader
                        )
                    }$POST"
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
                (if (spaced) "" else " ") + " ".repeat(shift) + "/* ${ZAXS.repeat(commentLength - shift)} */"
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
