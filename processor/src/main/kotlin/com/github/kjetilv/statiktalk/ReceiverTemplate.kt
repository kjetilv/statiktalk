package com.github.kjetilv.statiktalk

internal val receiverTemplate
    get() =
        """
package <packidge>

import com.github.kjetilv.statiktalk.api.ReceiveMediatorBase
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection

/*
  KMessage(
    packidge  : <packidge>
    service   : <service>
    servicelc : <servicelc>
    parameters: <parameters:{parameter|<parameter> }>
    contextual: <contextual>
  )
  contextClass: <contextClass>
*/

fun RapidsConnection.listen(<servicelc>: <service>) = <service>ReceiveMediator(<servicelc>).listenTo(this)

private class <service>ReceiveMediator(private val <servicelc>: <service>) : ReceiveMediatorBase() {

    override fun listenTo(connection: RapidsConnection) {
        val messageName = "<servicelc>"
        val parameters = <if(hasParams)>listOf(<parameters:{parameter|
            
            "<parameter>",}>
        )
        <else>emptyList\<String>()
        <endif>
        listen(connection, messageName, parameters)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
<parameters:{parameter|
        val <parameter> = packet["<parameter>"].textValue()
}>        <servicelc>.<name>(<parameters:{parameter|<parameter>, }><if(contextual)>context(packet, context)<else><endif>)
    }
}
""".trimIndent()
