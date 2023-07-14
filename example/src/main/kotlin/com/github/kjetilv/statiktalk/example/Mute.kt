package com.github.kjetilv.statiktalk.example

import com.github.kjetilv.statiktalk.api.Message
import com.github.kjetilv.statiktalk.api.Talk

@Talk
interface Mute {

    @Message
    fun shtum()
}
