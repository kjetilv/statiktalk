@file:Suppress("unused")

package com.github.kjetilv.statiktalk.processor

import com.google.devtools.ksp.symbol.KSFile

internal data class KService(
    val sourcePackidge: String,
    val packidge: String,
    val service: String,
    val containingFile: KSFile?
) {

    val qualifiedService: String get() = "${packidge}.${service}"

    val serviceCc = service.substring(0, 1).lowercase() + service.substring(1)
}
