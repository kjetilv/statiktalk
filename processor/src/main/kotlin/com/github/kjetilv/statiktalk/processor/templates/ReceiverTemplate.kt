package com.github.kjetilv.statiktalk.processor.templates

import com.github.kjetilv.statiktalk.processor.KMessage
import com.github.kjetilv.statiktalk.processor.KService

internal object ReceiverTemplate {

    private val receiverTemplate =
        """
/*
  This file was generated by statiktalk. Yes, we know, looks hand-written, don't it.
   
  Changes will be reverted when you rebuild, and you will like it.
    《if(imports)》
    
    Imports: 《imports:{import| 《import》 }》
    《endif》
        
    Message:
《ms:{m|
      《m》
}》
    Service: 
      《s》
*/
@file:Suppress("unused", "UNUSED_PARAMETER", "KotlinRedundantDiagnosticSuppress", "UnusedImport")

package 《s.packidge》

《imports:{import|
import 《import》
} 》
import com.github.kjetilv.statiktalk.api.ReceiveMediatorBase
import com.github.kjetilv.statiktalk.api.Req
import 《s.sourcePackidge》.《s.name》

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection

fun RapidsConnection.handle《s.name》(
    《s.nameCc》: 《s.name》,
    eventName: String? = null,
《if(ps)》
    reqs: 《s.name》Reqs? = null,    
《endif》
    customs: (JsonMessage.() -> Unit)? = null
) {
《ms:{m|
    《s.name》ReceiveMediator《m.upcasedServiceName》(《s.nameCc》)
        .listenTo(
            this,
            eventName,《if(ps)》 
            reqs, 
《endif》
            customs
        )
}》}

《if(ps)》
data class 《s.name》Reqs(
《ps:{p|
    val 《p.name》: Req<《p.type》>? = null};separator=",
    "》
)
《endif》

《ms:{m|

private class 《s.name》ReceiveMediator《m.upcasedServiceName》(
    private val 《s.nameCc》: 《s.name》
) : ReceiveMediatorBase() {

    fun listenTo(
        connection: RapidsConnection,
        eventName: String? = null,《if(ps)》 
        reqs: 《s.name》Reqs? = null,
《endif》
        customs: (JsonMessage.() -> Unit)? = null
    ) {
        val requiredKeys = 《if(m.hasRequiredKeys)》listOf(《m.requiredKeys:{requiredKey|
            
            "《requiredKey.name》",}》
        )
        《else》
            emptyList<String>()
        《endif》
        val interestingKeys = 《if(m.hasInterestingKeys)》listOf(《m.interestingKeys:{interestingKey|
            
            "《interestingKey.name》"};separator=",
            "》
        )
        《else》
            emptyList<String>()
        《endif》
        connection.listen(
            eventName《if(m.eventName)》 ?: "《m.eventName》"《endif》, 
            requiredKeys,
            interestingKeys,
            《if(ps)》
            mapOf(
《ps:{p|
                "《p.name》" to reqs?.《p.name》 };separator=",
                "》
            ),
            《else》
            emptyMap(), 
            《endif》
            customs
        )
    \}

    @Suppress("RedundantNullableReturnType", "RedundantSuppression")
    override fun onPacket(packet: JsonMessage, context: MessageContext) {
《m.requiredKeys:{requiredKey|
        val 《requiredKey.name》: 《requiredKey.type》 = 
            packet.resolveRequired("《requiredKey.name》", JsonNode::《requiredKey.jsonType》)
}》 
《m.interestingKeys:{interestingKey|
        val 《interestingKey.name》: 《interestingKey.type》? = 
            packet.resolve("《interestingKey.name》", JsonNode::《interestingKey.jsonType》)
}》
        《s.nameCc》.《m.name》(
《m.keys:{key|
            《key.name》};separator=",
            "》《if(m.contextual)》《if(m.hasKeys)》,
        《else》
        《endif》
            context(packet, context)
        《else》
        《endif》
        )
    \}
\}

}》
""".trimIndent()

    fun source(service: KService, messages: List<KMessage>) = receiverTemplate.source(service, messages)
}
