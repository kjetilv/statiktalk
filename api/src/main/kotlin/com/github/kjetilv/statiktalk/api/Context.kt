package com.github.kjetilv.statiktalk.api

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import java.math.BigDecimal

interface Context {
    object DUMMY : Context {
        override val packet: JsonMessage
            get() = throw IllegalStateException("Dummy context")
        override val context: MessageContext
            get() = throw IllegalStateException("Dummy context")
    }

    val packet: JsonMessage

    val context: MessageContext

    operator fun set(key: String, value: String) {
        packet[key] = value
    }

    operator fun set(key: String, value: Boolean) {
        packet[key] = value
    }

    operator fun set(key: String, value: Long) {
        packet[key] = value
    }

    operator fun set(key: String, value: BigDecimal) {
        packet[key] = value
    }
}
