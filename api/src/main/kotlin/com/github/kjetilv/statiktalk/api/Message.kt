@file:Suppress("unused")

package com.github.kjetilv.statiktalk.api

import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION

/**
 * Mark a function as a message.
 */
@Target(FUNCTION, CLASS)
@Retention(SOURCE)
annotation class Message(

    /**
     * Use this event name.  Overrides syntheticEventName.
     */
    val eventName: String = "",

    /**
     * Synthesize an event name from interface+function name.
     * By default, messages are routed by the parameter/field names only.
     */
    val syntheticEventName: Boolean = false
)
