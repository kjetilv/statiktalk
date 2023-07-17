package com.github.kjetilv.statiktalk.example

import com.github.kjetilv.statiktalk.api.Message
import java.math.BigDecimal
import java.math.BigInteger


interface Typed {

    @Message
    fun hello(
        name: String,
        fortyTwo: Int,
        fiftyFour: Long,
        word: Boolean,
        twice: Double,
        half: Float,
        precise: BigDecimal,
        big: BigInteger,
    )
}
