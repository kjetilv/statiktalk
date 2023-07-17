package com.github.kjetilv.statiktalk.example.testapp.shared

import com.github.kjetilv.statiktalk.api.Message

interface StatusCustomer {

    @Message
    fun status(userId: String, status: String)
}
