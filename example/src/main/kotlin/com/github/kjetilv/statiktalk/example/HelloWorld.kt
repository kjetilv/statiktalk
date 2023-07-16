package com.github.kjetilv.statiktalk.example

import com.github.kjetilv.statiktalk.api.Message

interface HelloWorld {

    @Message
    fun hello(name: String)
}
