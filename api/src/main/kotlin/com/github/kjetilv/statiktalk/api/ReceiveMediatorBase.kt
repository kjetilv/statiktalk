package com.github.kjetilv.statiktalk.api

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

abstract class ReceiveMediatorBase : River.PacketListener {

    protected fun listen(
        connection: RapidsConnection,
        eventName: String?,
        requiredKeys: List<String>,
        requiredValues: Map<String, Any?> = emptyMap(),
        interestingKeys: List<String> = emptyList(),
        additionalKeys: List<String> = emptyList()
    ) {
        River(connection).apply {
            validate { message ->
                eventName?.also {
                    message.requireValue("@event_name", it)
                }
                requiredKeys.filter { requiredValues[it] == null }
                    .forEach { key ->
                        message.requireKey(key)
                    }
                requiredValues.filterValues { value -> value != null }
                    .forEach { (key, value) ->
                        when (value) {
                            is Number -> message.requireValue(key, value)
                            is Boolean -> message.requireValue(key, value)
                            else -> message.requireValue(key, value.toString())
                        }
                    }
                (interestingKeys + additionalKeys).forEach { key ->
                    message.interestedIn(key)
                }
            }
        }.register(this)
    }

    protected fun context(packet: JsonMessage, context: MessageContext) =
        DefaultContext(packet, context)

    protected fun <T> JsonMessage.resolveRequired(name: String, resolver: (JsonNode) -> T): T =
        resolve(name, resolver) ?: throw IllegalStateException("Not found in $this: $name")

    protected fun <T> JsonMessage.resolve(name: String, resolver: (JsonNode) -> T): T? =
        get(name).let { node ->
            node.takeUnless { it.isNull }?.let(resolver)
        }
}
