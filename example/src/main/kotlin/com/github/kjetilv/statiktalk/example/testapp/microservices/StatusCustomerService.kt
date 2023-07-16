package com.github.kjetilv.statiktalk.example.testapp.microservices

import com.github.kjetilv.statiktalk.example.testapp.shared.StatusCustomer

class StatusCustomerService(private val sessions: Sessions) : StatusCustomer {

    override fun status(userId: String, status: String) =
        sessions.userChange(User(userId, status = status))
}
