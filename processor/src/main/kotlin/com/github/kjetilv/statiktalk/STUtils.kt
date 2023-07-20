package com.github.kjetilv.statiktalk

import org.stringtemplate.v4.ST
import java.math.BigDecimal
import java.math.BigInteger

internal fun String.source(service: KService, messages: List<KMessage>) =
    try {
        val imports = imports(messages)
        ST(this, '《', '》').apply {
            add("s", service)
            add("ms", messages)
            add("ps", combinedParams(messages))
            add("debug", true)
            add("imports", imports)
        }.render().replace(TRAILING, ")").trim()
    } catch (e: Exception) {
        throw IllegalStateException("Failed to render ${messages.size} messages", e)
    }

private fun imports(messages: List<KMessage>) =
    messages.flatMap { message ->
        message.keys.map { key ->
            key.type
        }
    }.distinct()
        .let { types ->
            explicit(types) + implicit(
                types,
                BigDecimal::class.java,
                BigInteger::class.java
            )
        }

private fun explicit(types: List<String>) =
    types.filter { type ->
        type.startsWith("java") || type.startsWith("kotlin")
    }

private fun implicit(types: List<String>, vararg implicits: Class<*>) =
    implicits.flatMap { implicit ->
        if (types.contains(implicit.simpleName))
            listOf(implicit.name)
        else
            emptyList()
    }

private fun combinedParams(messages: List<KMessage>) =
    messages.flatMap(KMessage::keys)
        .distinctBy(KParam::name)
        .map { (name, type) ->
            KParam(name, type)
        }

private val TRAILING = ",\\s+\\)".toRegex()
