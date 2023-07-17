package com.github.kjetilv.statiktalk.example.testapp.microservices

import com.github.kjetilv.statiktalk.api.Context

class SessionsService(private val memorySessions: MemorySessions) : Sessions {

    override fun loggedIn(userId: String, userKey: String, ctx: Context?) {
        memorySessions.loggedIn(User(userId, userKey))
    }

    override fun userChange(userId: String, userKey: String, returning: String?, status: String?, ctx: Context?) {
        memorySessions.userChange(User(userId, userKey, status, returning == "true"))
    }
}
