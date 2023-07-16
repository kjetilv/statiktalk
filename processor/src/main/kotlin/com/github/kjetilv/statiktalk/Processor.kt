package com.github.kjetilv.statiktalk

import com.github.kjetilv.statiktalk.api.Context
import com.github.kjetilv.statiktalk.api.Message
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.*
import org.stringtemplate.v4.ST
import java.io.PrintWriter

class Processor(private val codeGenerator: CodeGenerator) : SymbolProcessor {

    override fun process(resolver: Resolver) = emptyList<KSAnnotated>().also {
        contextType(resolver).let { contextType ->
            resolver.getSymbolsWithAnnotation(Message::class.java.name)
                .mapNotNull { it as? KSFunctionDeclaration }
                .forEach { functionDeclaration ->
                    declaringClass(functionDeclaration).let { classDeclaration ->
                        message(classDeclaration, functionDeclaration, contextType).let { message ->
                            with(
                                writer(
                                    classDeclaration,
                                    "${classDeclaration.simpleName.asString()}SenderMediator"
                                )
                            ) {
                                println(source(message, senderTemplate))
                            }
                            with(
                                writer(
                                    classDeclaration,
                                    "${classDeclaration.simpleName.asString()}ReceiverMediator"
                                )
                            ) {
                                println(source(message, receiverTemplate))
                            }
                        }
                    }
                }
        }
    }

    private fun declaringClass(ksFunctionDeclaration: KSFunctionDeclaration) =
        (ksFunctionDeclaration.parentDeclaration as? KSClassDeclaration)?.takeIf {
            it.classKind == ClassKind.INTERFACE
        } ?: throw IllegalStateException("An interface is needed to hold $ksFunctionDeclaration")

    private fun contextType(resolver: Resolver) =
        resolver.getClassDeclarationByName("com.github.kjetilv.statiktalk.api.Context")
            ?.qualifiedName
            ?: throw IllegalStateException("Could not resolve context type")

    private fun message(
        classDeclaration: KSClassDeclaration,
        functionDeclaration: KSFunctionDeclaration,
        contextType: KSName
    ): KMessage {
        val anno = functionDeclaration.annotations.first { annotation ->
            annotation.shortName.asString() == "Message"
        }
        val valueParameters = functionDeclaration.parameters
        val lastParam = valueParameters.lastOrNull()?.type
        val contextual = lastParam?.toString() == contextType
            .getShortName()
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
            classDeclaration.packageName.asString(),
            "${classDeclaration.packageName.asString()}.generated",
            classDeclaration.simpleName.asString(),
            functionDeclaration.simpleName.asString(),
            !parametersOnly,
            parameters,
            additionalKeys,
            contextual,
            contextNonNull
        )
    }

    private fun writer(declaration: KSClassDeclaration, className: String) =
        PrintWriter(mediatorClassFile(declaration, className), true)

    private fun source(message: KMessage, template: String) =
        try {
            ST(template, '〔', '〕').apply {
                add("sourcePackidge", message.sourcePackidge)
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

    private fun camelCase(name: String) = name.substring(0, 1).lowercase() + name.substring(1)

    private fun mediatorClassFile(decl: KSClassDeclaration, className: String) =
        codeGenerator.createNewFile(
            Dependencies(true, decl.containingFile!!),
            "${decl.packageName.asString()}.generated",
            className,
            "kt"
        )
}

