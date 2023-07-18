package com.github.kjetilv.statiktalk.example.testapp.microservices

import com.github.kjetilv.statiktalk.api.Context
import com.github.kjetilv.statiktalk.example.testapp.shared.Authorization
import com.github.kjetilv.statiktalk.example.testapp.shared.Sessions

class AuthorizationService(
    private val sessions: Sessions,
    private val authorized: Map<String, String>
) : Authorization {

    override fun userLoggedIn(userId: String, context: Context) {
        authorized[userId]?.also { key ->
            sessions.loggedIn(userId, key, context = context)
        }
    }
}
