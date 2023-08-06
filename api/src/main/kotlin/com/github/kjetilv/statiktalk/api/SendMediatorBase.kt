package com.github.kjetilv.statiktalk.api

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection

private const val eventNameKey = "@event_name"

abstract class SendMediatorBase(private val eventName: String? = null, private val connection: RapidsConnection) {

    protected fun send0(ctx: Context?, vararg contents: Pair<String, Any?>) =
            contents.toMap().also { ctx.publish(it) }

    protected fun send1(ctx: Context, vararg contents: Pair<String, Any?>) =
            contents.toMap().also { ctx.publish(it) }

    private fun Context?.publish(map: Map<String, Any?>) =
            (this as? DefaultContext)?.publishUpdated(map) ?: publishNew(map)

    private fun DefaultContext.publishUpdated(map: Map<String, Any?>) {
        nonNullValues(map).forEach { (key, value) -> packet[key] = value }
        eventName?.also { packet[eventNameKey] = it }
        context.publish(packet.toJson())
    }

    private fun publishNew(map: Map<String, Any?>) =
            nonNullValues(map).let { connection.publish(JsonMessage.newMessage(it).toJson()) }

    private fun nonNullValues(keys: Map<String, Any?>) =
            keys.filterValues { it != null }.mapValues { (_, it) -> it!! }
}
