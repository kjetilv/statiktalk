package com.github.kjetilv.ktalk.api

import kotlin.annotation.AnnotationTarget.FUNCTION

@Target(FUNCTION)
annotation class Message(
    val additionalKeys: Array<String> = []
)
