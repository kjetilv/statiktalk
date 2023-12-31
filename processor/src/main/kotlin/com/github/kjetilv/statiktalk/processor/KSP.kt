package com.github.kjetilv.statiktalk.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

internal fun CodeGenerator.mediatorClassFile(decl: KService, className: String) =
    createNewFile(
        decl.file
            ?.let { Dependencies(true, it) }
            ?: Dependencies(true),
        decl.packidge,
        className,
        "kt"
    )

internal val KSAnnotation.eventName get() = stringField("eventName")

internal fun KSAnnotation.syntheticEventName(service: KService, serviceName: String, keys: List<KParam>) =
    if (boolField("syntheticEventName") || keys.isEmpty()) "${service.name}_${serviceName}" else null

internal fun KSAnnotation.stringField(name: String) = field(name)?.toString()?.takeUnless { it.isBlank() }

private fun KSAnnotation.boolField(name: String) = field(name)?.let { it as? Boolean } ?: false

internal fun KSFunctionDeclaration.findAnno(name: String) =
    annotations.first { annotation -> annotation.shortName.asString() == name }

internal fun KSClassDeclaration.findAnno(name: String) =
    annotations.first { annotation -> annotation.shortName.asString() == name }

private fun KSAnnotation.field(name: String) = arguments.firstOrNull { it.name?.asString() == name }?.value
