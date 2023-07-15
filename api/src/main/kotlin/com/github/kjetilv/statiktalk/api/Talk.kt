package com.github.kjetilv.statiktalk.api

import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.CLASS

@Target(CLASS)
@Retention(SOURCE)
annotation class Talk
