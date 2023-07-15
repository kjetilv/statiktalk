package com.github.kjetilv.statiktalk.test

import com.github.kjetilv.statiktalk.api.Message
import com.github.kjetilv.statiktalk.api.Talk

@Talk
interface ReturningElite {

    @Message(parametersOnly = true)
    fun prodigalSon(returning: String, status: String)
}
