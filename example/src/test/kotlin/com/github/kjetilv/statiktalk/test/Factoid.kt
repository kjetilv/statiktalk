package com.github.kjetilv.statiktalk.test

import com.github.kjetilv.statiktalk.api.Context
import com.github.kjetilv.statiktalk.api.Message
import com.github.kjetilv.statiktalk.api.Talk

@Talk
interface Factoid {

    @Message
    fun annoyWith(subjectMatter: String, interestingFact: String, ctx: Context? = null)
}
