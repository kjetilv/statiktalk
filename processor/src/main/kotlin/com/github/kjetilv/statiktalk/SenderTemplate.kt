package com.github.kjetilv.statiktalk

internal val senderTemplate
    get() =
        """
package ⎨packidge⎬

import com.github.kjetilv.statiktalk.api.SendMediatorBase
import no.nav.helse.rapids_rivers.RapidsConnection

/*
  KMessage(
    packidge          : ⎨packidge⎬
    service           : ⎨service⎬
    serviceCc         : ⎨serviceCc⎬
    serviceName       : ⎨serviceName⎬
    requireServiceName: ⎨requireServiceName⎬
    parameters        : ⎨parameters:{parameter|⎨parameter⎬ }⎬
    additionalKeys    : ⎨additionalKeys:{additionalKey|⎨additionalKey⎬ }⎬
    contextual        : ⎨contextual⎬
    contextualNonNull : ⎨contextualNonNull⎬
  )
  hasParams         : ⎨hasParams⎬
  hasAdditionalKeys : ⎨hasAdditionalKeys⎬
  contextClass      : ⎨contextClass⎬
*/

fun RapidsConnection.new⎨service⎬(): ⎨service⎬ =
    ⎨service⎬SendMediator(this)

private class ⎨service⎬SendMediator(
    rapidsConnection: RapidsConnection
) : SendMediatorBase(rapidsConnection), ⎨service⎬ {

    override fun ⎨serviceName⎬(
⎨parameters:{parameter|
        ⎨parameter⎬: String,
        }⎬⎨if(contextual)
        ⎬        context: ⎨contextClass⎬⎨if(contextualNonNull)⎬⎨else⎬?⎨endif⎬
        ⎨else⎬⎨endif⎬
    ) {
        send⎨if(contextualNonNull)⎬1⎨else⎬0⎨endif⎬(
            ⎨if(contextual)⎬context⎨else⎬null⎨endif⎬,
            "@event_name" to "⎨service⎬_⎨serviceName⎬",
⎨parameters:{parameter|
            "⎨parameter⎬" to ⎨parameter⎬,
            }
⎬       )
    }
}
""".trimIndent()
