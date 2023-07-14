package com.github.kjetilv.statiktalk.api

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection

open class SendMediatorBase(private val rapidsConnection: RapidsConnection) {

    protected fun send(ctx: Context?, vararg contents: Pair<String, String>) =
        contents.toMap().also {
            if (ctx is DefaultContext) (ctx as? DefaultContext)?.apply(publishUpdated(it))
            else rapidsConnection.publish(JsonMessage.newMessage(it).toJson())
        }

    private fun publishUpdated(contents: Map<String, String>): DefaultContext.() -> Unit = {
        contents.entries.forEach { (key, value) ->
            packet[key] = value
        }
        context.publish(packet.toJson())
    }
}
