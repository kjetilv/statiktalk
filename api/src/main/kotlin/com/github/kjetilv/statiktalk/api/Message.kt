@file:Suppress("unused")

package com.github.kjetilv.statiktalk.api

import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.FUNCTION

/**
 * Mark a function as a message.
 */
@Target(FUNCTION)
@Retention(SOURCE)
annotation class Message(

    /**
     * Use this event name.  Overrides syntheticEventName.
     */
    val eventName: String = "",

    /**
     * Synthesize an event name from interface+function name. False by default, meaning that messages are routed
     * by the parameter names only.
     */
    val syntheticEventName: Boolean = false
)
