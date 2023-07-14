package com.github.kjetilv.statiktalk.api

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext

data class DefaultContext(
    override val packet: JsonMessage,
    override val context: MessageContext
) : Context {

}
