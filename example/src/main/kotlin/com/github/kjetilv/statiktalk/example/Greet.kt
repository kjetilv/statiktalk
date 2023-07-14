package com.github.kjetilv.statiktalk.example

import com.github.kjetilv.statiktalk.api.Context
import com.github.kjetilv.statiktalk.api.Message
import com.github.kjetilv.statiktalk.api.Talk

@Talk
interface Greet {

    @Message
    fun greet(name: String, context: Context? = null)
}
