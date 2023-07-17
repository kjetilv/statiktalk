package com.github.kjetilv.statiktalk.example.testapp.microservices

import com.github.kjetilv.statiktalk.api.Context
import com.github.kjetilv.statiktalk.example.testapp.shared.AuthorizedUserLoggedIn

class AuthorizedUserLoggedInService(
    private val sessions: Sessions,
    private val authorized: Map<String, String>
) : AuthorizedUserLoggedIn {

    override fun authorized(userId: String, context: Context) {
        authorized[userId]?.also { key ->
            sessions.loggedIn(userId, key, context)
        }
    }
}
