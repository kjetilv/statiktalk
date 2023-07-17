package com.github.kjetilv.statiktalk.example.testapp.microservices

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
    fun userChange(
        userId: String,
        userKey: String,
        returning: String? = null,
        status: String? = null,
        ctx: Context? = null
    )
}
