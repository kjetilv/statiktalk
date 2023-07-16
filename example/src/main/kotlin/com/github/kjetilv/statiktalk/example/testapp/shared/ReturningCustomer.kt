package com.github.kjetilv.statiktalk.example.testapp.shared

import com.github.kjetilv.statiktalk.api.Message

interface ReturningCustomer {

    @Message(parametersOnly = true)
    fun returning(userId: String, returning: String)
}
