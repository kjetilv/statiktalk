package com.github.kjetilv.statiktalk

internal val senderTemplate
    get() =
        """
package <packidge>

<if(contextual)>
import com.github.kjetilv.statiktalk.api.Context
<else><endif>
import com.github.kjetilv.statiktalk.api.SendMediatorBase
import no.nav.helse.rapids_rivers.RapidsConnection

/*
  KMessage(
    packidge  : <packidge>
    service   : <service>
    servicelc : <servicelc>
    parameters: <parameters:{parameter|<parameter> }>
    contextual: <contextual>
  )
*/

fun RapidsConnection.new<service>(): <service> = <service>SendMediator(this)

private class <service>SendMediator(
    rapidsConnection: RapidsConnection
) : SendMediatorBase(rapidsConnection), <service> {

    override fun <name>(
<parameters:{parameter|
        <parameter>: String,
        }><if(contextual)
        >        ctx: Context?
        <else><endif>
    ) {
        send(
            <if(contextual)>ctx<else>null<endif>,
            "@event_name" to "<servicelc>",
<parameters:{parameter|
            "<parameter>" to <parameter>,
            }
>       )
    }
}
""".trimIndent()
