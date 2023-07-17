package com.github.kjetilv.statiktalk.example.testapp.microservices

import com.github.kjetilv.statiktalk.example.testapp.shared.ReturningCustomer

class ReturningCustomerService(
    private val knownCustomers: Set<String> = emptySet(),
    private val sessions: Sessions
) : ReturningCustomer {

    override fun returning(userId: String, userKey: String) {
        if (knownCustomers.contains(userId)) {
            sessions.userChange(userId, userKey, "true")
        }
    }
}
