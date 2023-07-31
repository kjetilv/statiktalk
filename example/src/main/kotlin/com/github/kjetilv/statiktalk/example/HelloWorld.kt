package com.github.kjetilv.statiktalk.example

import com.github.kjetilv.statiktalk.api.Message

interface HelloWorld {

    @Message(syntheticEventName = true)
    fun hello(name: String, greeting: String? = null)
}
