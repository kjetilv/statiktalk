package com.github.kjetilv.statiktalk.example.testapp.shared

import com.github.kjetilv.statiktalk.api.Context
import com.github.kjetilv.statiktalk.api.Message

interface AuthorizedUserLoggedIn {

    @Message(
        requireEventName = true,
        additionalKeys = ["loginTime"]
    )
    fun authorized(userId: String, context: Context)
}
