package com.github.kjetilv.statiktalk.example.testapp.shared

import com.github.kjetilv.statiktalk.api.Context
import com.github.kjetilv.statiktalk.api.Message

interface LoginAttempt {

    @Message(requireEventName = true)
    fun loginAttempted(userId: String, context: Context = Context.DUMMY)
}
