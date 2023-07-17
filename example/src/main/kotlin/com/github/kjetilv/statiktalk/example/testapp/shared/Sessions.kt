package com.github.kjetilv.statiktalk.example.testapp.shared

import com.github.kjetilv.statiktalk.api.Context
import com.github.kjetilv.statiktalk.api.Message

interface Sessions {

    @Message(requireEventName = true)
    fun loggedIn(
        userId: String,
        userKey: String,
        ctx: Context? = null
    )

    @Message(requireEventName = true)
    fun userIsReturning(
        userId: String,
        userKey: String,
        returning: Boolean,
        ctx: Context? = null
    )

    @Message(requireEventName = true)
    fun userHasStatus(
        userId: String,
        userKey: String,
        status: String,
        ctx: Context? = null
    )
}
