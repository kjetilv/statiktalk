package com.github.kjetilv.statiktalk

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import org.stringtemplate.v4.ST
import java.io.PrintWriter

class Processor(private val codeGenerator: CodeGenerator) : SymbolProcessor {

    override fun process(resolver: Resolver) = emptyList<KSAnnotated>().also {
        val contextType =
            resolver.getClassDeclarationByName("com.github.kjetilv.statiktalk.api.Context")
                ?: throw IllegalStateException("Could not resolve context type")
        resolver.getSymbolsWithAnnotation("com.github.kjetilv.statiktalk.api.Talk")
            .mapNotNull { it as? KSClassDeclaration }
            .filter { it.classKind == ClassKind.INTERFACE }
            .distinctBy { it.simpleName }
            .forEach { decl ->
                message(decl, contextType).let { message ->
                    with(writer(decl, "Sender")) {
                        println(source(message, senderTemplate))
                    }
                    with(writer(decl, "Receiver")) {
                        println(source(message, receiverTemplate))
                    }
                }
            }
    }

    private fun message(decl: KSClassDeclaration, contextType: KSClassDeclaration) =
        decl.getAllFunctions()
            .filter { fd ->
                fd.annotations.any { annotation ->
                    annotation.shortName.asString() == "Message"
                }
            }
            .map { fd ->
                kMessage(decl, fd, contextType)
            }
            .toList()
            .takeIf { it.size == 1 }
            ?.firstOrNull()
            ?: throw IllegalStateException(
                "Only interfaces with exactly 1 function are supported, for now: " + decl.simpleName.asString()
            )

    private fun writer(decl: KSClassDeclaration, qualifier: String) =
        PrintWriter(mediatorClassFile(decl, qualifier), true)

    private fun source(message: KMessage, template: String): String {
        try {
            return ST(template).apply {
                add("packidge", message.packidge)
                add("service", message.service)
                add("name", message.name)
                add("servicelc", message.service.lowercase())
                add("parameters", message.parameters)
                add("contextual", message.contextual)
                add("hasParams", message.parameters.isNotEmpty())
            }.render()
        } catch (e: Exception) {
            throw IllegalStateException("Failed to render $message with $template", e)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun kMessage(
        decl: KSClassDeclaration,
        fd: KSFunctionDeclaration,
        contextType: KSClassDeclaration
    ): KMessage {
        val valueParameters = fd.parameters
        val contextual = valueParameters.lastOrNull()?.name?.asString() == "ctx"
        val parameters = valueParameters.let { if (contextual) it.dropLast(1) else it }
            .map { it.name }
            .map {
                requireNotNull(it) {
                    "Null element of parameters list: $valueParameters"
                }
            }
            .map { it.asString() }
        return KMessage(
            decl.packageName.asString(),
            decl.simpleName.asString(),
            fd.simpleName.asString(),
            parameters,
            contextual
        )
    }

    private fun mediatorClassFile(decl: KSClassDeclaration, qualifier: String) =
        codeGenerator.createNewFile(
            Dependencies(true, decl.containingFile!!),
            decl.packageName.asString(),
            "${decl.simpleName.asString()}${qualifier}Mediator",
            "kt"
        )
}

