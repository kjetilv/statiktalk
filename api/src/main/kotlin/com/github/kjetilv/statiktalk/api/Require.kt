package com.github.kjetilv.statiktalk.api

import com.github.kjetilv.statiktalk.api.Req.Kind.*
import com.github.kjetilv.statiktalk.api.Req.Kind.RequireValue

object Require {

    fun <T> value(required: T) = Req(required, RequireValue)

    fun <T> empty() = Req<T>(null, RejectKey)

    fun <T> notValue(rejected: T) = Req(rejected, RejectValue)
}
