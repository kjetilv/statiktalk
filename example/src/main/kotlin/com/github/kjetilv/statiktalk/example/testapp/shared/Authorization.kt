package com.github.kjetilv.statiktalk.example.testapp.shared

import com.github.kjetilv.statiktalk.api.Context
import com.github.kjetilv.statiktalk.api.Message

interface Authorization {

    @Message(eventName = "userAuthorization")
    fun userLoggedIn(userId: String, context: Context)
}
