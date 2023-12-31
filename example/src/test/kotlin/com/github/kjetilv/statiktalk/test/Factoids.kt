package com.github.kjetilv.statiktalk.test

import com.github.kjetilv.statiktalk.api.Context
import com.github.kjetilv.statiktalk.api.Message

interface Factoids {

    @Message(eventName = "factoids'r'us")
    fun annoyWith(
        subjectMatter: String,
        interestingFact: String,
        aside: String? = null,
        context: Context? = null
    )
}
