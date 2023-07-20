package com.github.kjetilv.statiktalk.example.testapp.shared

import com.github.kjetilv.statiktalk.api.Message

interface StatusProcessor {

    @Message
    fun status(userKey: String, status: String)
}
