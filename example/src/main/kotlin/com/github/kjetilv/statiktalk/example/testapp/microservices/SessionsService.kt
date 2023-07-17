package com.github.kjetilv.statiktalk.example.testapp.microservices

import com.github.kjetilv.statiktalk.api.Context
import com.github.kjetilv.statiktalk.example.testapp.shared.Sessions

class SessionsService(private val memorySessions: MemorySessions) : Sessions {

    override fun loggedIn(userId: String, userKey: String, ctx: Context?) {
        memorySessions.loggedIn(User(userId, userKey))
    }

    override fun userIsReturning(userId: String, userKey: String, returning: Boolean, ctx: Context?) {
        memorySessions.userChange(User(userId, userKey, status = null, returning))
    }

    override fun userHasStatus(userId: String, userKey: String, status: String, ctx: Context?) {
        memorySessions.userChange(User(userId, userKey, status = status))
    }
}
