@file:Suppress("unused")

package com.github.kjetilv.statiktalk.processor

import com.github.kjetilv.statiktalk.api.Context

internal data class KMessage(
    val serviceName: String,
    val eventName: String?,
    val keys: List<KParam>,
    val contextArg: String?,
    val contextualNullable: Boolean
) {

    val contextual = contextArg != null

    val hasKeys: Boolean = keys.isNotEmpty()

    val contextClass: String = contextClassName

    val upcasedServiceName = serviceName.substring(0, 1).uppercase() + serviceName.substring(1)

    val hasRequiredKeys get() = requiredKeys.isNotEmpty()

    val hasInterestingKeys get() = interestingKeys.isNotEmpty()

    val requiredKeys get() = keys.filterNot(KParam::optional)

    val interestingKeys get() = keys.filter(KParam::optional)
}

private val contextClassName = Context::class.java.name
