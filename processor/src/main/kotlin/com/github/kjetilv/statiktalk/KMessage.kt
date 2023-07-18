package com.github.kjetilv.statiktalk

import com.github.kjetilv.statiktalk.api.Context

@Suppress("unused")
internal data class KMessage(
    val serviceName: String,
    val eventName: String?,
    val keys: List<KParam>,
    val additionalKeys: List<String>,
    val contextual: Boolean,
    val contextualNullable: Boolean
) {

    val hasKeys: Boolean = keys.isNotEmpty()

    val contextClass: String = contextClassName

    val upcasedServiceName = serviceName.substring(0, 1).uppercase() + serviceName.substring(1)

    val hasRequiredKeys get() = requiredKeys.isNotEmpty()

    val hasInterestingKeys get() = interestingKeys.isNotEmpty()

    val requiredKeys get() = keys.filterNot(KParam::optional)

    val interestingKeys get() = keys.filter(KParam::optional)

    val hasAdditionalKeys get() = additionalKeys.isNotEmpty()
}

private val contextClassName = Context::class.java.name
