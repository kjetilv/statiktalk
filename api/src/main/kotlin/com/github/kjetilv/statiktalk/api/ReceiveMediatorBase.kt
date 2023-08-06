package com.github.kjetilv.statiktalk.api

import com.fasterxml.jackson.databind.JsonNode
import com.github.kjetilv.statiktalk.api.Req.Kind.Reject
import com.github.kjetilv.statiktalk.api.Req.Kind.Require
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

private const val eventNameKey = "@event_name"

abstract class ReceiveMediatorBase : River.PacketListener {

    protected fun RapidsConnection.listen(
            eventName: String?,
            requiredKeys: List<String> = emptyList(),
            keyValueRequirements: Map<String, Req<*>?> = emptyMap(),
            interestingKeys: List<String> = emptyList(),
            customs: (JsonMessage.() -> Unit)? = null
    ) {
        River(this).validate { message ->
            message.apply {
                registerEventName(eventName)
                registerRequiredKeys(requiredKeys.filter(notIn(keyValueRequirements)))
                registerRequiredValues(keyValueRequirements)
                registerRejectedValues(keyValueRequirements)
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
            eventName?.also { requireValue(eventNameKey, it) }

    private fun JsonMessage.registerRequiredKeys(keys: List<String>) =
            keys.forEach { key -> requireKey(key) }

    private fun JsonMessage.registerRequiredValues(requiredValues: Map<String, Req<*>?>) =
            requiredValues.ofType(Require) { key, value ->
                when (value) {
                    is Number -> requireValue(key, value)
                    is Boolean -> requireValue(key, value)
                    else -> value.apply { requireValue(key, toString()) }
                }
            }

    private fun JsonMessage.registerRejectedValues(requiredValues: Map<String, Req<*>?>) =
            requiredValues.ofType(Reject) { key, value ->
                when (value) {
                    is Boolean -> rejectValue(key, value)
                    else -> value.apply { rejectValue(key, toString()) }
                }
            }

    private fun Map<String, Req<*>?>.ofType(kind: Req.Kind, add: (String, Any) -> Unit) =
            filterValues { req -> req?.kind == kind }
                    .mapValues { (_, req) -> req?.value }
                    .filterValues { value -> value != null }
                    .mapValues { (_, value) -> value!! }
                    .forEach(add)

    private fun JsonMessage.registerInterestingKeys(interestingKeys: List<String>) =
            interestingKeys.forEach { key -> interestedIn(key) }

    private fun notIn(requiredValues: Map<String, Any?>): (String) -> Boolean = { requiredValues[it] == null }
}
