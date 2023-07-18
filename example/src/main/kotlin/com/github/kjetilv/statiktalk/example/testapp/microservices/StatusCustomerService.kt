package com.github.kjetilv.statiktalk.example.testapp.microservices

import com.github.kjetilv.statiktalk.example.testapp.shared.AuthorizedUserEnricher
import com.github.kjetilv.statiktalk.example.testapp.shared.Sessions

class StatusCustomerService(
    private val sessions: Sessions,
    private val statii: Map<String, String>
) : AuthorizedUserEnricher {

    override fun authorized(userId: String, userKey: String) {
        statii[userId]?.also {
            sessions.userHasStatus(userId, userKey, it)
        }
    }
}
