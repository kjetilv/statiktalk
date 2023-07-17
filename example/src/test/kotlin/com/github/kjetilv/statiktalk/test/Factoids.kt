package com.github.kjetilv.statiktalk.test

import com.github.kjetilv.statiktalk.api.Context
import com.github.kjetilv.statiktalk.api.Message

interface Factoids {

    @Message(requireEventName = true)
    fun annoyWith(subjectMatter: String, interestingFact: String, context: Context? = null)
}
