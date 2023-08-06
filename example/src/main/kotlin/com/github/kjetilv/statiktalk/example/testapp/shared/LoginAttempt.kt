package com.github.kjetilv.statiktalk.example.testapp.shared

import com.github.kjetilv.statiktalk.api.Context
import com.github.kjetilv.statiktalk.api.Message

interface LoginAttempt {

    @Message(eventName = "userLogin")
    fun loginAttempted(
            userId: String,
            channel: String? = null,
            browser: String? = null,
            context: Context = Context.DUMMY
    )
}
