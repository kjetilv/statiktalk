package com.github.kjetilv.statiktalk.test

import com.github.kjetilv.statiktalk.api.Context
import com.github.kjetilv.statiktalk.api.Message
import com.github.kjetilv.statiktalk.api.Talk

@Talk
interface HighValueUserLoggedIn {

    @Message
    fun loggedInWithStatus(userId: String, status: String, context: Context? = null)
}
