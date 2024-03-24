package com.github.kjetilv.statiktalk.api

import com.fasterxml.jackson.databind.JsonNode
import com.github.kjetilv.statiktalk.api.Req.Kind
import com.github.kjetilv.statiktalk.api.Req.Kind.*
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River


abstract class ReceiveMediatorBase : River.PacketListener {

    protected fun RapidsConnection.listen(
        eventName: String?,
        requiredKeys: List<String> = emptyList(),
        interestingKeys: List<String> = emptyList(),
        reqs: Map<String, Req<*>?> = emptyMap(),
        customs: (JsonMessage.() -> Unit)? = null
    ) {
        River(this).validate { message ->
            message.apply {
                registerEventName(eventName)
                registerRequiredKeys(requiredKeys.filter { reqs[it] == null })
                registerRequiredValues(reqs)
                registerRejectedKeys(reqs)
                registerRejectedValues(reqs)
                registerInterestingKeys(interestingKeys)
                customs?.also { it.invoke(message) }
            }
        }.register(this@ReceiveMediatorBase)
    }

    protected fun context(packet: JsonMessage, context: MessageContext) = DefaultContext(packet, context)

    protected fun <T> JsonMessage.resolveRequired(name: String, resolver: (JsonNode) -> T) =
        resolve(name, resolver) ?: throw IllegalStateException("Not found in $this: $name")

    protected fun <T> JsonMessage.resolve(name: String, resolver: (JsonNode) -> T) =
        get(name).takeUnless(JsonNode::isNull)?.let(resolver)

    private fun JsonMessage.registerEventName(eventName: String?) =
        eventName?.also { requireValue("@event_name", it) }

    private fun JsonMessage.registerRequiredKeys(keys: List<String>) =
        keys.forEach { key -> requireKey(key) }

    private fun JsonMessage.registerRequiredValues(requiredValues: Map<String, Req<*>?>) =
        requiredValues.filterFor(RequireValue) { key, value ->
            when (value) {
                is Number -> requireValue(key, value)
                is Boolean -> requireValue(key, value)
                else -> value.apply { requireValue(key, toString()) }
            }
        }

    private fun JsonMessage.registerRejectedKeys(requiredValues: Map<String, Req<*>?>) =
        requiredValues.filterKind(RejectKey).forEach { (key: String, _) -> rejectKey(key) }

    private fun JsonMessage.registerRejectedValues(requiredValues: Map<String, Req<*>?>) =
        requiredValues.filterFor(RejectValue) { key, value ->
            when (value) {
                is Boolean -> rejectValue(key, value)
                else -> value.apply { rejectValue(key, toString()) }
            }
        }

    private fun Map<String, Req<*>?>.filterFor(kind: Kind, action: (String, Any) -> Unit) =
        filterKind(kind).mapValues { (_, req) -> req?.value }
            .filterValues { it != null }
            .mapValues { (_, value) -> value!! }
            .forEach(action)

    private fun Map<String, Req<*>?>.filterKind(kind: Kind) = filterValues { it?.kind == kind }

    private fun JsonMessage.registerInterestingKeys(interestingKeys: List<String>) =
        interestingKeys.forEach { key -> interestedIn(key) }
}
