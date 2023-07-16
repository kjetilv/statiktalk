package com.github.kjetilv.statiktalk

import com.google.devtools.ksp.symbol.KSFile

data class KService(
    val sourcePackidge: String,
    val packidge: String,
    val service: String,
    val containingFile: KSFile?
) {

    val serviceCc get() = camelCase(service)

    private fun camelCase(name: String) = name.substring(0, 1).lowercase() + name.substring(1)
}
