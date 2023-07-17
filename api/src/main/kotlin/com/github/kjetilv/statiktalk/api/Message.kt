package com.github.kjetilv.statiktalk.api

import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.FUNCTION

/**
 * Mark a function as a message.
 */
@Suppress("unused")
@Target(FUNCTION)
@Retention(SOURCE)
annotation class Message(

    /**
     * Use this event name.  Overrides simpleEventName and fullEventName.
     */
    val eventName: String = "",

    /**
     * Concatenate service name and function name as event name.  Overrides simpleEventName.
     */
    val fullEventName: Boolean = false,

    /**
     * Use function name as event name.
     */
    val simpleEventName: Boolean = false,

    val additionalKeys: Array<String> = []
)
