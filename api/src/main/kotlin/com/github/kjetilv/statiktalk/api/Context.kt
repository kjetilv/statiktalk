@file:Suppress("unused")

package com.github.kjetilv.statiktalk.api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.*
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.isMissingOrNull
import java.math.BigDecimal


interface Context {

    object DUMMY : Context {

        override val packet: JsonMessage get() = throw IllegalStateException("Dummy context")

        override val context: MessageContext get() = throw IllegalStateException("Dummy context")
    }

    val packet: JsonMessage

    val context: MessageContext

    operator fun set(key: String, value: String) { packet[key] = value }

    operator fun set(key: String, value: Boolean) { packet[key] = value }

    operator fun set(key: String, value: Long) { packet[key] = value }

    operator fun set(key: String, value: BigDecimal) { packet[key] = value }

    operator fun get(key: String) =
            node(key)?.let {
                when (it) {
                    is TextNode -> it.textValue()
                    is BooleanNode -> it.booleanValue()
                    is BigIntegerNode -> it.bigIntegerValue()
                    is DecimalNode -> it.decimalValue()
                    is NumericNode -> it.numberValue()
                    is NullNode, is MissingNode -> null
                    else ->
                        throw IllegalStateException("$key has unsupported node type: " + it.nodeType)
                }
            }

    fun getString(key: String) = node(key)?.textValue()

    fun getInt(key: String) = node(key)?.intValue()

    fun getLong(key: String) = node(key)?.longValue()

    fun getFloat(key: String) = node(key)?.floatValue()

    fun isTrue(key: String) = node(key)?.booleanValue()

    fun getDouble(key: String) = node(key)?.doubleValue()

    fun getBigDecimal(key: String) = node(key)?.decimalValue()

    fun getBigInteger(key: String) = node(key)?.bigIntegerValue()

    private fun node(key: String): JsonNode? = packet[key].takeUnless { it.isMissingOrNull() }
}
