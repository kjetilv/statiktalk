package com.github.kjetilv.statiktalk.example.testapp.microservices

import com.github.kjetilv.statiktalk.example.testapp.shared.StatusCustomer

class StatusCustomerService(
    private val statii: Map<String, String>,
    private val sessions: Sessions
) : StatusCustomer {

    override fun status(userId: String, userKey: String) {
        statii[userId]?.also {
            sessions.userChange(userId = userId, userKey = userKey, status = it)
        }
    }
}
