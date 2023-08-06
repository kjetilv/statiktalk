package com.github.kjetilv.statiktalk.api

data class Req<T>(val value: T?, val kind: Kind) {

    enum class Kind {
        RequireValue,
        RejectKey,
        RejectValue
    }
}
