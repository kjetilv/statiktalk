@file:Suppress("unused")

package com.github.kjetilv.statiktalk.processor

import com.github.kjetilv.statiktalk.api.Context

internal data class KMessage(
    val name: String,
    val eventName: String?,
    val keys: List<KParam>,
    val contextArg: String?,
    val contextualNullable: Boolean
) {

    val contextual = contextArg != null

    val hasKeys: Boolean = keys.isNotEmpty()

    val contextClass: String = contextClassName

    val upcasedServiceName = name.substring(0, 1).uppercase() + name.substring(1)

    val hasRequiredKeys get() = requiredKeys.isNotEmpty()

    val hasInterestingKeys get() = interestingKeys.isNotEmpty()

    val requiredKeys get() = keys.filterNot(KParam::optional)

    val interestingKeys get() = keys.filter(KParam::optional)

    override fun toString() =
        "$name/$eventName${
            keys.ifEmpty { "[]]" }
        }${
            if(contextualNullable) " nullable" else ""
        }${
            contextArg?.let { " $it" } ?: ""
        }"
}

private val contextClassName = Context::class.java.simpleName
