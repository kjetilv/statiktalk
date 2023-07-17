package com.github.kjetilv.statiktalk.example.testapp.microservices

import com.github.kjetilv.statiktalk.api.Context
import com.github.kjetilv.statiktalk.api.Message

interface Sessions {

    @Message
    fun loggedIn(userId: String, ctx: Context? = null)

    @Message
    fun userChange(userId: String, returning: String? = null, status: String? = null, ctx: Context? = null)
}
