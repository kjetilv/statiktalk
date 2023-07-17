package com.github.kjetilv.statiktalk

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
            functionMap(resolver, contextType).forEach { (service, messages) ->
                messages.forEach { message ->
                    with(
                        writer(
                            service,
                            "${service.service}SenderMediator"
                        )
                    ) {
                        println(source(service, message, senderTemplate))
                    }
                    with(
                        writer(
                            service,
                            "${service.service}ReceiverMediator"
                        )
                    ) {
                        println(source(service, message, receiverTemplate))
                    }
                }
            }
        }
    }

    private fun functionMap(resolver: Resolver, contextType: KSName) =
        resolver.getSymbolsWithAnnotation(Message::class.java.name)
            .mapNotNull { it as? KSFunctionDeclaration }
            .groupBy { declaringClass(it) }
            .mapKeys { (key, _) ->
                ksService(key)
            }
            .mapValues { (_, functionDeclarations) ->
                functionDeclarations.map { functionDeclaration ->
                    message(functionDeclaration, contextType)
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

    private fun ksService(classDeclaration: KSClassDeclaration) = KService(
        classDeclaration.packageName.asString(),
        "${classDeclaration.packageName.asString()}.generated",
        classDeclaration.simpleName.asString(),
        classDeclaration.containingFile
    )

    private fun message(
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
        val contextNullable =
            !contextual || (lastParam?.resolve()?.isMarkedNullable ?: false)
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
            functionDeclaration.simpleName.asString(),
            !parametersOnly,
            parameters,
            additionalKeys,
            contextual,
            contextNullable
        )
    }

    private fun writer(service: KService, className: String) =
        PrintWriter(codeGenerator.mediatorClassFile(service, className), true)

    private fun source(service: KService, message: KMessage, template: String) =
        try {
            ST(template, '〔', '〕').apply {
                add("s", service)
                add("m", message)
                add("debug", true)
            }.render().replace(",\\s+\\)".toRegex(), ")")
        } catch (e: Exception) {
            throw IllegalStateException("Failed to render $message with $template", e)
        }
}

private fun CodeGenerator.mediatorClassFile(decl: KService, className: String) =
    createNewFile(
        decl.containingFile
            ?.let { Dependencies(true, it) }
            ?: Dependencies(true),
        decl.packidge,
        className,
        "kt"
    )

