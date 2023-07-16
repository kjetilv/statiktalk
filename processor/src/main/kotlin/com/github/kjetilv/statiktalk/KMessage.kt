package com.github.kjetilv.statiktalk

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
)
