package com.github.kjetilv.statiktalk.api

import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

abstract class ReceiveMediatorBase : River.PacketListener {

    abstract fun listenTo(connection: RapidsConnection)

    protected fun listen(connection: RapidsConnection, messageName: String, fields: List<String>) {
        River(connection).apply {
            validate { message ->
                message.requireValue("@event_name", messageName)
            }
            validate { message ->
                fields.forEach { field ->
                    message.requireKey(field)
                }
            }
        }.register(this)
    }
}
