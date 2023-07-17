package com.github.kjetilv.statiktalk.example

import com.github.kjetilv.statiktalk.api.Message

interface HelloWorld {

    @Message(fullEventName = true)
    fun hello(name: String)
}
