package com.github.kjetilv.statiktalk.example

import com.github.kjetilv.statiktalk.api.Context
import com.github.kjetilv.statiktalk.api.Message

interface Greet {

    @Message
    fun greet(name: String, context: Context? = null)
}
