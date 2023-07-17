package com.github.kjetilv.statiktalk.test

import com.github.kjetilv.statiktalk.api.Context
import com.github.kjetilv.statiktalk.api.Message

interface Factoids {

    @Message(fullEventName = true)
    fun annoyWith(
        subjectMatter: String,
        interestingFact: String,
        aside: String? = null,
        context: Context? = null
    )
}
