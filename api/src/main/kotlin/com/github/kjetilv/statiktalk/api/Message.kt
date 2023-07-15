package com.github.kjetilv.statiktalk.api

import kotlin.annotation.AnnotationTarget.FUNCTION

@Suppress("unused")
@Target(FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Message(
    val parametersOnly: Boolean = false,
    val additionalKeys: Array<String> = []
)
