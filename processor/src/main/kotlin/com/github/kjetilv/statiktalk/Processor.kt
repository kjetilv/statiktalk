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
import java.math.BigDecimal
import java.math.BigInteger

class Processor(private val codeGenerator: CodeGenerator) : SymbolProcessor {

    override fun process(resolver: Resolver) = emptyList<KSAnnotated>().also {
        contextType(resolver).let { contextType ->
            functionMap(resolver, contextType).forEach { (service, messages) ->
                with(
                    writer(
                        service,
                        "${service.service}SenderMediator"
                    )
                ) {
                    println(source(service, messages, senderTemplate))
                }
                with(
                    writer(
                        service,
                        "${service.service}ReceiverMediator"
                    )
                ) {
                    println(source(service, messages, receiverTemplate))
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
            .mapValues { (service, functionDeclarations) ->
                functionDeclarations.map { functionDeclaration ->
                    message(service, functionDeclaration, contextType)
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
        service: KService,
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
        val keys = valueParameters.let { if (contextual) it.dropLast(1) else it }
            .map {
                KParam(
                    it.name?.asString() ?: throw IllegalStateException("Null name: $it"),
                    it.type.element.toString(),
                    it.type.resolve().isMarkedNullable
                )
            }
        val fullEventName = anno.arguments
            .first { it.name?.asString() == "fullEventName" }
            .let { it.value as? Boolean }
            ?: false
        val simpleEventName = anno.arguments
            .first { it.name?.asString() == "simpleEventName" }
            .let { it.value as? Boolean }
            ?: false
        val eventName = anno.arguments
            .firstOrNull { it.name?.asString() == "eventName" }
            ?.value
            ?.toString()
            ?.takeUnless { it.isBlank() }
        val additionalKeys = anno.arguments
            .first { it.name?.asString() == "additionalKeys" }
            .let { it.value as List<*> }
            .map { it.toString() }
        val serviceName = functionDeclaration.simpleName.asString()
        return KMessage(
            serviceName,
            eventName ?: (
                    if (fullEventName) "${service.service}_${serviceName}"
                    else if (simpleEventName) serviceName
                    else null
                    ),
            keys,
            additionalKeys,
            contextual,
            contextNullable
        )
    }

    private fun writer(service: KService, className: String) =
        PrintWriter(codeGenerator.mediatorClassFile(service, className), true)

    private fun source(service: KService, messages: List<KMessage>, template: String) =
        try {
            val imports = imports(messages)
            ST(template, '《', '》').apply {
                add("s", service)
                add("ms", messages)
                add("debug", true)
                add("imports", imports)
            }.render().replace(TRAILING, ")").trim()
        } catch (e: Exception) {
            throw IllegalStateException(
                "Failed to render ${messages.size} messages with $template", e
            )
        }

    private fun imports(messages: List<KMessage>): List<String> =
        messages.flatMap { message ->
            message.keys.map { key ->
                key.type
            }
        }.distinct()
            .let { types ->
                explicit(types) + implicit(
                    types,
                    BigDecimal::class.java,
                    BigInteger::class.java
                ) + jsonNode(messages)
            }

    private fun explicit(types: List<String>) =
        types.filter { type ->
            type.startsWith("java") || type.startsWith("kotlin")
        }

    private fun implicit(types: List<String>, vararg implicits: Class<*>) =
        implicits.flatMap { implicit ->
            if (types.contains(implicit.simpleName))
                listOf(implicit.name)
            else
                emptyList()
        }

    private fun jsonNode(messages: List<KMessage>): List<String> =
        if (messages.any { it.hasKeys }) {
            listOf("com.fasterxml.jackson.databind.JsonNode")
        } else {
            emptyList()
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

private val TRAILING = ",\\s+\\)".toRegex()
