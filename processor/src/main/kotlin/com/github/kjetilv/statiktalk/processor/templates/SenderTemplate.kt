package com.github.kjetilv.statiktalk.processor.templates

import com.github.kjetilv.statiktalk.processor.KMessage
import com.github.kjetilv.statiktalk.processor.KService

internal object SenderTemplate {

    private val senderTemplate =
        """
@file:Suppress("unused", "UNUSED_PARAMETER", "KotlinRedundantDiagnosticSuppress")

package 《s.packidge》

《imports:{import|
import 《import》
} 》
import com.github.kjetilv.statiktalk.api.SendMediatorBase
import 《s.sourcePackidge》.《s.name》

import no.nav.helse.rapids_rivers.RapidsConnection

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

fun RapidsConnection.《s.nameCc》(eventName: String? = null, action: (《s.name》.() -> Unit)? = null): 《s.name》 =
    《s.name》SendMediator(rapidsConnection = this, eventName = eventName).apply {
        action?.invoke(this)
    }

private class 《s.name》SendMediator(
    rapidsConnection: RapidsConnection,
    eventName: String? = null
) : SendMediatorBase(eventName, rapidsConnection), 《s.name》 {

《ms:{m|
    override fun 《m.name》(
《m.keys:{key|
        《key.name》: 《key.type》《if(key.optional)》?《endif》};separator=",
        "》《if(m.contextual)
        》《if(m.hasKeys)》,《endif》
        《m.contextArg》: 《m.contextClass》《if(m.contextualNullable)》?《endif》
        《else》《endif》

    ) {
        send《if(m.contextualNullable)》0《else》1《endif》(
            ctx = 《if(m.contextual)》《m.contextArg》《else》null《endif》,
            《if(m.eventName)》"@event_name" to "《m.eventName》"《if(m.hasKeys)》,《endif》《endif》
《m.keys:{key|
            "《key.name》" to 《key.name》};separator=",
            "》
        )
    \}
}》}
""".trimIndent()

    fun source(service: KService, messages: List<KMessage>) = senderTemplate.source(service, messages)
}
