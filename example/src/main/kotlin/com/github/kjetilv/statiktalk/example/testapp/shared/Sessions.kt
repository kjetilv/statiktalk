package com.github.kjetilv.statiktalk.example.testapp.shared

import com.github.kjetilv.statiktalk.api.Context
import com.github.kjetilv.statiktalk.api.Message

@Message(syntheticEventName = true)
interface Sessions {

    fun loggedIn(
        userId: String,
        userKey: String,
        loginTime: String? = null,
        context: Context
    )

    fun userIsReturning(
        userId: String,
        userKey: String,
        returning: Boolean,
    )

    fun userHasStatus(
        userId: String,
        userKey: String,
        status: String,
    )
}
