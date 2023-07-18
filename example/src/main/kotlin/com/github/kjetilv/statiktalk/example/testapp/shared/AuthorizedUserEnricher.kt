package com.github.kjetilv.statiktalk.example.testapp.shared

import com.github.kjetilv.statiktalk.api.Message

interface AuthorizedUserEnricher {

    @Message
    fun authorized(userId: String, userKey: String)
}
