package com.github.kjetilv.statiktalk.example.testapp.microservices

import com.github.kjetilv.statiktalk.api.Context
import com.github.kjetilv.statiktalk.example.testapp.shared.AuthorizedUserLoggedIn

class AuthorizedUserLoggedInService(
    private val sessions: Sessions,
    vararg actions: Pair<String, List<() -> Unit>>
) : AuthorizedUserLoggedIn {

    private val actionMap = mapOf(*actions)

    private val authorized = actionMap.keys

    override fun authorized(userId: String, context: Context) {
        if (authorized.contains(userId)) {
            sessions.loggedIn(
                User(
                    userId,
                    metadata = mapOf(
                        "loginTime" to context.packet["loginTime"].textValue()
                    )
                )
            )
            actionMap[userId]?.forEach { it() }
        }
    }
}
