package com.github.kjetilv.statiktalk.example.testapp.shared

import com.github.kjetilv.statiktalk.api.Context
import com.github.kjetilv.statiktalk.api.Message

interface Unauthorized {

    @Message(syntheticEventName = true)
    fun unknownuser(userId: String, loginTime: String? = null, context: Context)
}
