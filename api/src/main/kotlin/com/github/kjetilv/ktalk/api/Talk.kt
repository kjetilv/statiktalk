package com.github.kjetilv.ktalk.api

import kotlin.annotation.AnnotationTarget.CLASS

@Target(CLASS)
annotation class Talk(
    val additionalKeys: Array<String> = []
) {
}
