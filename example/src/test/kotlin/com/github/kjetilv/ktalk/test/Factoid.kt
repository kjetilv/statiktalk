package com.github.kjetilv.ktalk.test

import com.github.kjetilv.ktalk.api.Context
import com.github.kjetilv.ktalk.api.Message
import com.github.kjetilv.ktalk.api.Talk

@Talk
interface Factoid {

    @Message
    fun annoyWith(subjectMatter: String, interestingFact: String, ctx: Context? = null)
}
