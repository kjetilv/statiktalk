package com.github.kjetilv.statiktalk.api

object Require {

    fun <T> value(required: T) = Req(required, Req.Kind.Require)

    fun <T> notValue(rejected: T) = Req(rejected, Req.Kind.Reject)
}
