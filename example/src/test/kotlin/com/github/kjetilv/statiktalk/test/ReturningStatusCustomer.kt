package com.github.kjetilv.statiktalk.test

import com.github.kjetilv.statiktalk.api.Message
import com.github.kjetilv.statiktalk.api.Talk

@Talk
interface ReturningStatusCustomer {

    @Message(parametersOnly = true)
    fun returning(returning: String, status: String)
}
