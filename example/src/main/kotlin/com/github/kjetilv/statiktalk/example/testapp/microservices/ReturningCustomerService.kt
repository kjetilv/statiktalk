package com.github.kjetilv.statiktalk.example.testapp.microservices

import com.github.kjetilv.statiktalk.example.testapp.shared.ReturningCustomer
import kotlin.String

class ReturningCustomerService(private val sessions: Sessions) : ReturningCustomer {

    override fun returning(userId: String, returning: String) =
        sessions.userChange(User(userId, returning = returning == "true"))
}
