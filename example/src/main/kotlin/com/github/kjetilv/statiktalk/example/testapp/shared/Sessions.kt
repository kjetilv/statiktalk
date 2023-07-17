package com.github.kjetilv.statiktalk.example.testapp.shared

import com.github.kjetilv.statiktalk.api.Context
import com.github.kjetilv.statiktalk.api.Message

interface Sessions {

    @Message(simpleEventName = true)
    fun loggedIn(
        userId: String,
        userKey: String,
        ctx: Context? = null
    )

    @Message(simpleEventName = true)
    fun userIsReturning(
        userId: String,
        userKey: String,
        returning: Boolean,
        ctx: Context? = null
    )

    @Message(simpleEventName = true)
    fun userHasStatus(
        userId: String,
        userKey: String,
        status: String,
        ctx: Context? = null
    )
}
