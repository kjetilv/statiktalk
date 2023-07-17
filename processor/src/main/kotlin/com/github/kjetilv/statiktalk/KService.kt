package com.github.kjetilv.statiktalk

import com.google.devtools.ksp.symbol.KSFile

@Suppress("unused")
data class KService(
    val sourcePackidge: String,
    val packidge: String,
    val service: String,
    val containingFile: KSFile?
) {

    val serviceCc = service.substring(0, 1).lowercase() + service.substring(1)
}
