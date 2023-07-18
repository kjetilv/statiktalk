package com.github.kjetilv.statiktalk

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

internal fun KSAnnotation.stringField(name: String) =
    field(name)?.toString()?.takeUnless { it.isBlank() }

internal fun KSAnnotation.stringsField(name: String) =
    field(name).let { it as List<*> }.map { it.toString() }

internal fun KSAnnotation.boolField(name: String) =
    field(name)?.let { it as? Boolean } ?: false

internal fun KSAnnotation.field(name: String) =
    arguments.firstOrNull() { it.name?.asString() == name }?.value

internal fun KSFunctionDeclaration.findAnno(name: String) =
    annotations.first { annotation ->
        annotation.shortName.asString() == name
    }

internal fun CodeGenerator.mediatorClassFile(decl: KService, className: String) =
    createNewFile(
        decl.containingFile
            ?.let { Dependencies(true, it) }
            ?: Dependencies(true),
        decl.packidge,
        className,
        "kt"
    )
