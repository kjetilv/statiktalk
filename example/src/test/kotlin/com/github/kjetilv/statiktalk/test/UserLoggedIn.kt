package com.github.kjetilv.statiktalk.test

import com.github.kjetilv.statiktalk.api.Context
import com.github.kjetilv.statiktalk.api.Message
import com.github.kjetilv.statiktalk.api.Talk

@Talk
interface UserLoggedIn {

    @Message
    fun loggedIn(userId: String, returning: String, context: Context? = null)
}
