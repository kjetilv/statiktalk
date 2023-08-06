package com.github.kjetilv.statiktalk.api

data class Req<T>(val value: T, val kind: Kind) {

    enum class Kind {
        Require,
        Reject
    }
}
