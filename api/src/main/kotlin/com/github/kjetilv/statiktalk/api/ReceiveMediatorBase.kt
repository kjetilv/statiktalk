package com.github.kjetilv.statiktalk.api

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

abstract class ReceiveMediatorBase : River.PacketListener {

    abstract fun listenTo(connection: RapidsConnection, additionalKeys: List<String> = emptyList())

    protected fun listen(
        connection: RapidsConnection,
        eventName: String?,
        requiredKeys: List<String>,
        interestingKeys: List<String>,
        additionalKeys: List<String>
    ) {
        River(connection).apply {
            validate { message ->
                eventName?.also {
                    message.requireValue("@event_name", it)
                }
                requiredKeys.forEach { key ->
                    message.requireKey(key)
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
