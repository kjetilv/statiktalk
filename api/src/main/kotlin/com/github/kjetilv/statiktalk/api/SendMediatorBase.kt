package com.github.kjetilv.statiktalk.api

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.JsonMessage.Companion.newMessage
import no.nav.helse.rapids_rivers.RapidsConnection

private const val eventNameKey = "@event_name"

@Suppress("unused")
abstract class SendMediatorBase(private val eventName: String? = null, private val connection: RapidsConnection) {

    protected fun send0(ctx: Context?, vararg contents: Pair<String, Any?>) = send0(ctx, contents.toList())

    protected fun send0(ctx: Context?, contents: List<Pair<String, Any?>>) =
        contents.toMap().also { ctx.publish(it) }

    protected fun send1(ctx: Context, vararg contents: Pair<String, Any?>) = send1(ctx, contents.toList())

    protected fun send1(ctx: Context, contents: List<Pair<String, Any?>>) =
        contents.toMap().also { ctx.publish(it) }

    private fun Context?.publish(map: Map<String, Any?>) =
        (this as? DefaultContext)?.publishUpdated(map) ?: publishNew(map)

    private fun DefaultContext.publishUpdated(map: Map<String, Any?>) {
        nonNullValues(map).forEach { (key, value) ->
            packet[key] = value
        }
        context.publish(applyEventName(packet).toJson())
    }

    private fun publishNew(map: Map<String, Any?>) =
        applyEventName(newMessage(nonNullValues(map))).toJson().run(connection::publish)

    private fun applyEventName(jsonMessage: JsonMessage) =
        jsonMessage.also { eventName?.also { eventName -> it[eventNameKey] = eventName } }

    private fun nonNullValues(keys: Map<String, Any?>) =
        keys.filterValues { it != null }.mapValues { (_, it) -> it!! }
}
