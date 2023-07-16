package com.github.kjetilv.statiktalk.example.testapp.microservices

data class User(
    val userId: String,
    val status: String? = null,
    val returning: Boolean? = null,
    val metadata: Map<String, String>? = emptyMap()
)
