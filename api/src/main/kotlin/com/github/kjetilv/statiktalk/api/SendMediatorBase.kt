package com.github.kjetilv.statiktalk.api

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection

open class SendMediatorBase(private val rapidsConnection: RapidsConnection) {

    protected fun send0(ctx: Context?, vararg contents: Pair<String, String?>) =
        contents.toMap().also { ctx.publish(it) }

    protected fun send1(ctx: Context, vararg contents: Pair<String, String?>) =
        contents.toMap().also { ctx.publish(it) }

    private fun Context?.publish(keys: Map<String, String?>) {
        if (this is DefaultContext) (this as? DefaultContext)?.apply(publishUpdated(keys))
        else rapidsConnection.publish(JsonMessage.newMessage(realValues(keys)).toJson())
    }

    private fun realValues(keys: Map<String, String?>) =
        keys.filterValues { it != null }.mapValues { (_, it) -> it!! }

    private fun publishUpdated(contents: Map<String, String?>): DefaultContext.() -> Unit = {
        contents.entries.forEach { (key, value) ->
            value?.also { packet[key] = it }
        }
        context.publish(packet.toJson())
    }
}
