package com.github.kjetilv.statiktalk.processor.templates

import com.github.kjetilv.statiktalk.processor.KMessage
import com.github.kjetilv.statiktalk.processor.KService
import com.github.kjetilv.statiktalk.processor.st.source

internal object ReceiverTemplate {

    private val receiverTemplate =
        """
@file:Suppress("unused", "UNUSED_PARAMETER", "KotlinRedundantDiagnosticSuppress", "UnusedImport")

package 《s.packidge》

《imports:{import|
import 《import》
} 》
import com.github.kjetilv.statiktalk.api.ReceiveMediatorBase
import 《s.sourcePackidge》.《s.name》

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection

/*
  This file was generated by statiktalk. Changes will be reverted when you rebuild.
  《if(debug) 》
    《if(imports)》
    
    Imports: 《imports:{import| 《import》 }》
    《endif》
        
    Message:
《ms:{m|
      《m》
}》
    Service: 
      《s》
    《endif》
*/

fun RapidsConnection.handle《s.name》(
    《s.nameCc》: 《s.name》,
    eventName: String? = null,
《if(ps)》
    reqs: 《s.name》Reqs? = null,    
《endif》
    vararg additionalKeys: String
) {
《ms:{m|
    《s.name》ReceiveMediator《m.upcasedServiceName》(《s.nameCc》)
        .listenTo(
            this,
            eventName, 
《if(ps)》
            reqs,    
《endif》
            additionalKeys.toList()
        )
}》}

《if(ps)》
data class 《s.name》Reqs(
《ps:{p|
    val 《p.name》: 《p.type》? = null};separator=",
    "》
)
《endif》

《ms:{m|

private class 《s.name》ReceiveMediator《m.upcasedServiceName》(
    private val 《s.nameCc》: 《s.name》
) : ReceiveMediatorBase() {

    fun listenTo(
        connection: RapidsConnection,
        eventName: String? = null, 
《if(ps)》
        reqs: 《s.name》Reqs? = null,    
《endif》
        additionalKeys: List<String>
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
            requiredKeys = requiredKeys,
            《if(ps)》
            requiredValues = mapOf(
《ps:{p|
                "《p.name》" to reqs?.《p.name》, 
}》
            ),
            《else》
            requiredValues = emptyMap(), 
            《endif》
            interestingKeys = interestingKeys,
            additionalKeys = additionalKeys)
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