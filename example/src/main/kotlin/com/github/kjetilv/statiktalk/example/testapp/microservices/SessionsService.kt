package com.github.kjetilv.statiktalk.example.testapp.microservices

import com.github.kjetilv.statiktalk.api.Context
import com.github.kjetilv.statiktalk.example.testapp.shared.SessionDb
import com.github.kjetilv.statiktalk.example.testapp.shared.Sessions
import com.github.kjetilv.statiktalk.example.testapp.shared.User

class SessionsService(private val sessionDb: SessionDb, private val eventLog: (String) -> Unit) : Sessions {

    override fun loggedIn(userId: String, userKey: String, loginTime: String?, context: Context) {
        try {
            sessionDb.loggedIn(User(userId, userKey))
        } finally {
            eventLog("Login was registered at $loginTime")
        }
    }

    override fun userIsReturning(userId: String, userKey: String, returning: Boolean) {
        sessionDb.userChange(User(userId, userKey, status = null, returning))
    }

    override fun userHasStatus(userId: String, userKey: String, status: String) {
        sessionDb.userChange(User(userId, userKey, status = status))
    }
}
