package com.github.kjetilv.statiktalk.api

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection

private const val eventNameKey = "@event_name"

abstract class SendMediatorBase(private val eventName: String? = null, private val connection: RapidsConnection) {

    protected fun send0(ctx: Context?, vararg contents: Pair<String, Any?>) =
        contents.toMap().also { ctx.publish(it) }

    protected fun send1(ctx: Context, vararg contents: Pair<String, Any?>) =
        contents.toMap().also { ctx.publish(it) }

    private fun Context?.publish(keys: Map<String, Any?>) =
        if (this is DefaultContext) publishUpdated(keys) else publishNew(keys)

    private fun Context?.publishUpdated(keys: Map<String, Any?>) {
        (this as? DefaultContext)?.apply {
            keys.entries.forEach { (key, value) -> value?.also { packet[key] = it } }
            eventName?.also { packet[eventNameKey] = it }
            context.publish(packet.toJson())
        }
    }

    private fun publishNew(keys: Map<String, Any?>) = keys.filterValues { it != null }
        .mapValues { (_, it) -> it!! }
        .let { connection.publish(JsonMessage.newMessage(it).toJson()) }
}
