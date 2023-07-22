package com.github.kjetilv.statiktalk.example.testapp.shared

import com.github.kjetilv.statiktalk.api.Context

class SessionsDao(private val sessionDb: SessionDb, private val eventLog: (String) -> Unit) : Sessions {

    override fun loggedIn(userId: String, userKey: String, loginTime: String?, context: Context) =
        try {
            sessionDb.loggedIn(User(userId, userKey))
        } finally {
            eventLog("Login was registered at $loginTime")
        }

    override fun userIsReturning(userId: String, userKey: String, returning: Boolean) =
        sessionDb.userChange(User(userId, userKey, status = null, returning))

    override fun userHasStatus(userId: String, userKey: String, status: String) =
        sessionDb.userChange(User(userId, userKey, status = status))
}
