package com.github.kjetilv.statiktalk

import com.github.kjetilv.statiktalk.api.Context
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.ClassKind.INTERFACE
import org.stringtemplate.v4.ST
import java.io.PrintWriter

class Processor(private val codeGenerator: CodeGenerator) : SymbolProcessor {

    override fun process(resolver: Resolver) = emptyList<KSAnnotated>().also {
        contextType(resolver).let { contextType ->
            resolver.getSymbolsWithAnnotation("com.github.kjetilv.statiktalk.api.Talk")
                .mapNotNull { it as? KSClassDeclaration }
                .filter { it.classKind == INTERFACE }
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
    }

    private fun contextType(resolver: Resolver) =
        resolver.getClassDeclarationByName("com.github.kjetilv.statiktalk.api.Context")
            ?.qualifiedName
            ?: throw IllegalStateException("Could not resolve context type")

    private fun message(decl: KSClassDeclaration, contextType: KSName) =
        decl.getAllFunctions().filter { fd ->
            fd.annotations.any { annotation ->
                annotation.shortName.asString() == "Message"
            }
        }.map { fd ->
            kMessage(
                decl,
                fd,
                fd.annotations.first { annotation ->
                    annotation.shortName.asString() == "Message"
                },
                contextType
            )
        }.toList()
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
                add("serviceCc", camelCase(message.service))
                add("serviceName", message.name)
                add("requireServiceName", message.requireServiceName)
                add("parameters", message.parameters)
                add("contextual", message.contextual)
                add("contextualNonNull", message.contextualNonNull)
                add("additionalKeys", message.additionalKeys)
                add("contextClass", Context::class.java.name)
                add("hasParams", message.parameters.isNotEmpty())
                add("hasAdditionalKeys", message.additionalKeys.isNotEmpty())
            }.render().replace(",\\s+\\)".toRegex(), ")")
        } catch (e: Exception) {
            throw IllegalStateException("Failed to render $message with $template", e)
        }
    }

    private fun camelCase(name: String) = name.substring(0, 1).lowercase() + name.substring(1)

    private fun kMessage(
        decl: KSClassDeclaration,
        fd: KSFunctionDeclaration,
        anno: KSAnnotation,
        contextType: KSName
    ): KMessage {
        val valueParameters = fd.parameters
        val lastParam = valueParameters.lastOrNull()?.type
        val contextual = lastParam?.toString() == contextType.getShortName()
        val contextNonNull = contextual && !(lastParam?.resolve()?.isMarkedNullable ?: false)
        val parameters = valueParameters.let { if (contextual) it.dropLast(1) else it }
            .map { it.name }
            .map {
                requireNotNull(it) {
                    "Null element of parameters list: $valueParameters"
                }
            }
            .map { it.asString() }
        val parametersOnly = anno.arguments
            .first { it.name?.asString() == "parametersOnly" }
            .let { it.value as? Boolean }
            ?: false
        val additionalKeys = anno.arguments
            .first { it.name?.asString() == "additionalKeys" }
            .let { it.value as List<*> }
            .map { it.toString() }
        return KMessage(
            decl.packageName.asString(),
            decl.simpleName.asString(),
            fd.simpleName.asString(),
            !parametersOnly,
            parameters,
            additionalKeys,
            contextual,
            contextNonNull
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

