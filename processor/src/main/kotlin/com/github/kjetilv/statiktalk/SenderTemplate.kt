package com.github.kjetilv.statiktalk

internal val senderTemplate
    get() =
        """
package <packidge>

import com.github.kjetilv.statiktalk.api.SendMediatorBase
import no.nav.helse.rapids_rivers.RapidsConnection

/*
  KMessage(
    packidge          : <packidge>
    service           : <service>
    servicelc         : <servicelc>
    servicename       : <servicename>
    parameters        : <parameters:{parameter|<parameter> }>
    contextual        : <contextual>
    contextualNonNull : <contextualNonNull>
  )             
  contextClass: <contextClass>
*/

fun RapidsConnection.new<service>(): <service> =
    <service>SendMediator(this)

private class <service>SendMediator(
    rapidsConnection: RapidsConnection
) : SendMediatorBase(rapidsConnection), <service> {

    override fun <servicename>(
<parameters:{parameter|
        <parameter>: String,
        }><if(contextual)
        >        context: <contextClass><if(contextualNonNull)><else>?<endif>
        <else><endif>
    ) {
        send<if(contextualNonNull)>1<else>0<endif>(
            <if(contextual)>context<else>null<endif>,
            "@event_name" to "<service>_<servicename>",
<parameters:{parameter|
            "<parameter>" to <parameter>,
            }
>       )
    }
}
""".trimIndent()
