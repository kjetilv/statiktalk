package com.github.kjetilv.statiktalk.example

import com.github.kjetilv.statiktalk.api.Context
import com.github.kjetilv.statiktalk.api.Message

interface Greet {

    @Message(syntheticEventName = true)
    fun greet(name: String, greeting: String? = null, context: Context? = null)
}
