package com.github.kjetilv.statiktalk.api

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext

data class DefaultContext(val packet: JsonMessage, val context: MessageContext) : Context {
}
