package com.github.kjetilv.ktalk.example

import com.github.kjetilv.ktalk.api.Message
import com.github.kjetilv.ktalk.api.Talk

@Talk
interface Mute {

    @Message
    fun shtum()
}
