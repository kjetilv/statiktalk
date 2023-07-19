package com.github.kjetilv.statiktalk


internal class SenderTemplate {

    companion object {
        private val senderTemplate
            get() =
                """
@file:Suppress("unused", "UNUSED_PARAMETER", "KotlinRedundantDiagnosticSuppress")

package 《s.packidge》

《imports:{import|
import 《import》
} 》
import com.github.kjetilv.statiktalk.api.SendMediatorBase

import 《s.sourcePackidge》.《s.service》

import no.nav.helse.rapids_rivers.RapidsConnection

/*
  This file was generated by statiktalk. Changes will be reverted when you rebuild.
  《if(debug) 》
    
    Imports: 《imports:{import| 《import》 }》
    
    Message:
《ms:{m|
      《m》
}》
    Service: 《s》
    《 endif》
*/

fun RapidsConnection.《s.serviceCc》(): 《s.service》 =
    《s.service》SendMediator(this)

private class 《s.service》SendMediator(
    rapidsConnection: RapidsConnection
) : SendMediatorBase(rapidsConnection), 《s.service》 {

《ms:{m|
    override fun 《m.serviceName》(
《m.keys:{key|
        《key.name》: 《key.type》《if(key.optional)》?《endif》,
        }》《if(m.contextual)
        》        《m.contextArg》: 《m.contextClass》《if(m.contextualNullable)》?《else》《endif》
        《else》《endif》
    ) {
        send《if(m.contextualNullable)》0《else》1《endif》(
            《if(m.contextual)》《m.contextArg》《else》null《endif》,
            《if(m.eventName)》"@event_name" to "《m.eventName》",《endif》
《m.keys:{key|
            "《key.name》" to 《key.name》,
            }》)
    \}
}》}
""".trimIndent()

        internal fun source(service: KService, messages: List<KMessage>) = senderTemplate.source(service, messages)
    }
}
