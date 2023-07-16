package com.github.kjetilv.statiktalk

import com.github.kjetilv.statiktalk.api.Context

data class KMessage(
    val sourcePackidge: String,
    val packidge: String,
    val service: String,
    val name: String,
    val requireServiceName: Boolean,
    val parameters: List<String>,
    val additionalKeys: List<String>,
    val contextual: Boolean,
    val contextualNonNull: Boolean
) {

    val serviceCc get() = camelCase(service)

    val contextClass = contextClassName

    val hasParams get() = parameters.isNotEmpty()

    val hasAdditionalKeys get() = additionalKeys.isNotEmpty()

    private fun camelCase(name: String) = name.substring(0, 1).lowercase() + name.substring(1)
}

private val contextClassName = Context::class.java.name
