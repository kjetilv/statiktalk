package com.github.kjetilv.statiktalk.example.testapp.microservices

import com.github.kjetilv.statiktalk.api.Context
import com.github.kjetilv.statiktalk.example.testapp.shared.Sessions

class SessionsService(private val sessionDb: SessionDb) : Sessions {

    override fun loggedIn(userId: String, userKey: String, ctx: Context?) {
        sessionDb.loggedIn(User(userId, userKey))
    }

    override fun userIsReturning(userId: String, userKey: String, returning: Boolean, ctx: Context?) {
        sessionDb.userChange(User(userId, userKey, status = null, returning))
    }

    override fun userHasStatus(userId: String, userKey: String, status: String, ctx: Context?) {
        sessionDb.userChange(User(userId, userKey, status = status))
    }
}
