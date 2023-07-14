package com.github.kjetilv.statiktalk

data class KMessage(
    val packidge: String,
    val service: String,
    val name: String,
    val parameters: List<String>,
    val contextual: Boolean = false
)
