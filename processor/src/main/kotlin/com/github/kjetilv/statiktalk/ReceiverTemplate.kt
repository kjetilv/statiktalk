package com.github.kjetilv.statiktalk

internal val receiverTemplate
    get() =
        """
package ⁅packidge⁆

import com.github.kjetilv.statiktalk.api.ReceiveMediatorBase
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection

/*
  KMessage(
    packidge          : ⁅packidge⁆
    service           : ⁅service⁆
    serviceCc         : ⁅serviceCc⁆
    serviceName       : ⁅serviceName⁆
    parameters        : ⁅parameters:{parameter|⁅parameter⁆ }⁆
    additionalKeys    : ⁅additionalKeys:{additionalKey|⁅additionalKey⁆ }⁆
    contextual        : ⁅contextual⁆
    contextualNonNull : ⁅contextualNonNull⁆
  )
  hasParams         : ⁅hasParams⁆
  hasAdditionalKeys : ⁅hasAdditionalKeys⁆
  contextClass      : ⁅contextClass⁆
*/

fun RapidsConnection.listen(⁅serviceCc⁆: ⁅service⁆, vararg interestingKeys: String) =
    ⁅service⁆ReceiveMediator(⁅serviceCc⁆).listenTo(this, interestingKeys.toList())

private class ⁅service⁆ReceiveMediator(
    private val ⁅serviceCc⁆: ⁅service⁆
) : ReceiveMediatorBase() {

    override fun listenTo(connection: RapidsConnection, optionalKeys: List<String>) {
        val parameters = ⁅if(hasParams)⁆listOf(⁅parameters:{parameter|
            
            "⁅parameter⁆",}⁆
        )
        ⁅else⁆emptyList<String>()
        ⁅endif⁆
        listen(
            connection, 
            ⁅if(requireServiceName)⁆"⁅service⁆_⁅serviceName⁆"⁅else⁆null⁅endif⁆, 
            parameters,
            ⁅if(hasAdditionalKeys)⁆
            listOf(⁅additionalKeys:{additionalKey|"⁅additionalKey⁆", }⁆),
            ⁅else⁆
            emptyList(),
            ⁅endif⁆
            optionalKeys)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
⁅parameters:{parameter|
        val ⁅parameter⁆ = packet["⁅parameter⁆"].textValue()
}⁆        ⁅serviceCc⁆.⁅serviceName⁆(⁅parameters:{parameter|⁅parameter⁆, }⁆⁅if(contextual)⁆context(packet, context)⁅else⁆⁅endif⁆)
    }
}
""".trimIndent()
