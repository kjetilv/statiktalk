package com.github.kjetilv.statiktalk

import com.github.kjetilv.statiktalk.api.Context

@Suppress("unused")
data class KMessage(
    val serviceName: String,
    val requireServiceName: Boolean,
    val parameters: List<String>,
    val additionalKeys: List<String>,
    val contextual: Boolean,
    val contextualNullable: Boolean
) {

    val contextClass: String = contextClassName

    val hasParams get() = parameters.isNotEmpty()

    val hasAdditionalKeys get() = additionalKeys.isNotEmpty()
}

private val contextClassName = Context::class.java.name
