@file:Suppress("unused")

package com.github.kjetilv.statiktalk.api

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import java.math.BigDecimal

data class DefaultContext(override val packet: JsonMessage, override val context: MessageContext) : Context

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

    fun getString(key: String) = get(key)?.textValue()

    fun getInt(key: String) = get(key)?.intValue()

    fun getLong(key: String) = get(key)?.longValue()

    fun getFloat(key: String) = get(key)?.floatValue()

    fun isTrue(key: String) = get(key)?.booleanValue()

    fun getDouble(key: String) = get(key)?.doubleValue()

    fun getBigDecimal(key: String) = get(key)?.decimalValue()

    fun getBigInteger(key: String) = get(key)?.bigIntegerValue()

    private fun get(key: String): JsonNode? = packet[key].takeUnless { it.isNull }
}
