package com.github.kjetilv.statiktalk.api

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext

interface Context {

    val packet: JsonMessage

    val context: MessageContext
}
