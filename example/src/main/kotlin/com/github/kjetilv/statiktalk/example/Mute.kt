package com.github.kjetilv.statiktalk.example

import com.github.kjetilv.statiktalk.api.Context
import com.github.kjetilv.statiktalk.api.Message

interface Mute {

    @Message
    fun shtum(context: Context? = null)
}
