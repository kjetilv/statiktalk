package com.github.kjetilv.statiktalk.example.testapp.shared

import com.github.kjetilv.statiktalk.api.Message

interface ReturningCustomer {

    @Message
    fun returning(userId: String, userKey: String)
}
