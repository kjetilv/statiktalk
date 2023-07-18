package com.github.kjetilv.statiktalk.example.testapp.shared

import com.github.kjetilv.statiktalk.api.Context
import com.github.kjetilv.statiktalk.api.Message

interface Sessions {

    @Message(simpleEventName = true)
    fun loggedIn(
        userId: String,
        userKey: String,
        loginTime: String? = null,
        context: Context
    )

    @Message(simpleEventName = true)
    fun userIsReturning(
        userId: String,
        userKey: String,
        returning: Boolean,
    )

    @Message(simpleEventName = true)
    fun userHasStatus(
        userId: String,
        userKey: String,
        status: String,
    )
}
