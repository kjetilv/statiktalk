package com.github.kjetilv.statiktalk

import com.github.kjetilv.statiktalk.api.Context

data class KMessage(
    val name: String,
    val requireServiceName: Boolean,
    val parameters: List<String>,
    val additionalKeys: List<String>,
    val contextual: Boolean,
    val contextualNonNull: Boolean
) {

    val contextClass = contextClassName

    val hasParams get() = parameters.isNotEmpty()

    val hasAdditionalKeys get() = additionalKeys.isNotEmpty()

}

private val contextClassName = Context::class.java.name
