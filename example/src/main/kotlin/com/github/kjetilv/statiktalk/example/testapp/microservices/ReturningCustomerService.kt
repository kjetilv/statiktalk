package com.github.kjetilv.statiktalk.example.testapp.microservices

import com.github.kjetilv.statiktalk.example.testapp.shared.AuthorizedUserEnricher
import com.github.kjetilv.statiktalk.example.testapp.shared.Sessions

class ReturningCustomerService(
    private val sessions: Sessions,
    private val knownCustomers: Set<String> = emptySet()
) : AuthorizedUserEnricher {

    override fun authorized(userId: String, userKey: String) {
        if (knownCustomers.contains(userId)) {
            sessions.userIsReturning(userId, userKey, true)
        }
    }
}
