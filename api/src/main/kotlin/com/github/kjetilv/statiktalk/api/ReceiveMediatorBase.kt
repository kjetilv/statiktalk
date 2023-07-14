package com.github.kjetilv.statiktalk.api

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

abstract class ReceiveMediatorBase : River.PacketListener {

    abstract fun listenTo(connection: RapidsConnection, optionalKeys: List<String> = emptyList())

    protected fun listen(
        connection: RapidsConnection,
        eventName: String,
        requiredKeys: List<String>,
        interestingKeys: List<String>
    ) {
        River(connection).apply {
            validate { message ->
                message.requireValue("@event_name", eventName)
                requiredKeys.forEach { key ->
                    message.requireKey(key)
                }
                interestingKeys.forEach { key ->
                    message.interestedIn(key)
                }
            }
        }.register(this)
    }

    protected fun context(packet: JsonMessage, context: MessageContext) =
        DefaultContext(packet, context)
}
