package com.github.kjetilv.statiktalk.example.testapp.microservices

import com.github.kjetilv.statiktalk.example.testapp.shared.Sessions
import com.github.kjetilv.statiktalk.example.testapp.shared.StatusCustomer

class StatusCustomerService(
    private val sessions: Sessions,
    private val statii: Map<String, String>
) : StatusCustomer {

    override fun status(userId: String, userKey: String) {
        statii[userId]?.also {
            sessions.userHasStatus(userId, userKey, it)
        }
    }
}
