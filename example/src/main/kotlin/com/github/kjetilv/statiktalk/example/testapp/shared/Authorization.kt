package com.github.kjetilv.statiktalk.example.testapp.shared

import com.github.kjetilv.statiktalk.api.Context
import com.github.kjetilv.statiktalk.api.Message

interface Authorization {

    @Message(requireEventName = true)
    fun userLoggedIn(userId: String, context: Context)
}
