package com.github.kjetilv.statiktalk.api

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

private const val eventNameKey = "@event_name"

abstract class ReceiveMediatorBase : River.PacketListener {

    protected fun RapidsConnection.listen(
        eventName: String?,
        requiredKeys: List<String> = emptyList(),
        requiredValues: Map<String, Any?> = emptyMap(),
        interestingKeys: List<String> = emptyList(),
        additionalKeys: List<String> = emptyList()
    ) {
        River(this).validate { message ->
            message.apply {
                registerEventName(eventName)
                registerRequiredKeys(requiredKeys.filter(notIn(requiredValues)))
                registerRequiredValues(requiredValues)
                registerInterestingKeys(interestingKeys, additionalKeys)
            }
        }.register(this@ReceiveMediatorBase)
    }

    private fun JsonMessage.registerEventName(eventName: String?) =
        eventName?.also {
            requireValue(eventNameKey, it)
        }

    private fun notIn(requiredValues: Map<String, Any?>): (String) -> Boolean = {
        requiredValues[it] == null
    }

    private fun JsonMessage.registerRequiredKeys(keys: List<String>) = keys.forEach { key ->
        requireKey(key)
    }

    private fun JsonMessage.registerRequiredValues(requiredValues: Map<String, Any?>) =
        requiredValues.filterValues { value -> value != null }
            .forEach { (key, value) ->
                when (value) {
                    is Number -> requireValue(key, value)
                    is Boolean -> requireValue(key, value)
                    else -> requireValue(key, value.toString())
                }
            }

    private fun JsonMessage.registerInterestingKeys(interestingKeys: List<String>, additionalKeys: List<String>) =
        (interestingKeys + additionalKeys).forEach { key ->
            interestedIn(key)
        }

    protected fun context(packet: JsonMessage, context: MessageContext) =
        DefaultContext(packet, context)

    protected fun <T> JsonMessage.resolveRequired(name: String, resolver: (JsonNode) -> T): T =
        resolve(name, resolver) ?: throw IllegalStateException("Not found in $this: $name")

    protected fun <T> JsonMessage.resolve(name: String, resolver: (JsonNode) -> T) =
        get(name).takeUnless(JsonNode::isNull)?.let(resolver)
}
