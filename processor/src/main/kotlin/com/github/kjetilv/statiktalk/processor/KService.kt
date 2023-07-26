@file:Suppress("unused")

package com.github.kjetilv.statiktalk.processor

import com.google.devtools.ksp.symbol.KSFile

internal data class KService(
    val sourcePackidge: String,
    val packidge: String,
    val name: String,
    val file: KSFile?
) {

    val qualifiedService: String get() = "${packidge}.${name}"

    val nameCc = name.substring(0, 1).lowercase() + name.substring(1)
}
