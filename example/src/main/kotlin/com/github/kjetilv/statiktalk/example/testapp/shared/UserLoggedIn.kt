package com.github.kjetilv.statiktalk.example.testapp.shared

import com.github.kjetilv.statiktalk.api.Context
import com.github.kjetilv.statiktalk.api.Message

interface UserLoggedIn {

    @Message(requireEventName = true)
    fun loggedIn(userId: String, context: Context? = null)
}
