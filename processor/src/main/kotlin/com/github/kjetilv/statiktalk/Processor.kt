package com.github.kjetilv.statiktalk

import com.github.kjetilv.statiktalk.api.Message
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.*
import java.io.PrintWriter

class Processor(private val codeGenerator: CodeGenerator) : SymbolProcessor {

    override fun process(resolver: Resolver) = emptyList<KSAnnotated>().also {
        contextType(resolver).let { contextType ->
            functionMap(resolver, contextType).forEach { (service, messages) ->
                with(writer(service, "${service.service}SenderMediator")) {
                    println(senderTemplate.generateSource(service, messages))
                }
                with(writer(service, "${service.service}ReceiverMediator")) {
                    println(receiverTemplate.generateSource(service, messages))
                }
            }
        }
    }

    private fun functionMap(resolver: Resolver, contextType: KSName) =
        try {
            resolver.getSymbolsWithAnnotation(ANNOTATION_NAME)
                .mapNotNull { it as? KSFunctionDeclaration }
                .groupBy { declaringClass(it) }
                .mapKeys { (classDeclaration, _) ->
                    kService(classDeclaration)
                }
                .mapValues { (service, functionDeclarations) ->
                    functionDeclarations.map {
                        kMessage(service, verified(service, it), contextType)
                    }
                }
        } catch (e: Exception) {
            throw IllegalStateException("Failed to process messages", e)
        }

    private fun declaringClass(ksFunctionDeclaration: KSFunctionDeclaration) =
        (ksFunctionDeclaration.parentDeclaration as? KSClassDeclaration)?.takeIf {
            it.classKind == ClassKind.INTERFACE
        } ?: throw IllegalStateException("An interface is needed to hold $ksFunctionDeclaration")

    private fun contextType(resolver: Resolver) =
        resolver.getClassDeclarationByName("com.github.kjetilv.statiktalk.api.Context")
            ?.qualifiedName
            ?: throw IllegalStateException("Could not resolve context type")

    private fun kService(classDeclaration: KSClassDeclaration) = KService(
        classDeclaration.packageName.asString(),
        "${classDeclaration.packageName.asString()}.generated",
        classDeclaration.simpleName.asString(),
        classDeclaration.containingFile
    )

    private fun kMessage(service: KService, decl: KSFunctionDeclaration, contextType: KSName): KMessage {
        val anno = decl.findAnno(ANNOTATION_SHORT_NAME)
        val valueParameters = decl.parameters
        val lastParam = valueParameters.lastOrNull()?.type
        val contextual = lastParam?.toString() == contextType.getShortName()
        val contextNullable = !contextual || (lastParam?.resolve()?.isMarkedNullable ?: false)
        val keys = valueParameters
            .let { if (contextual) it.dropLast(1) else it }
            .map(::kParam)
        val serviceName = decl.simpleName.asString()
        val eventName = anno.stringField("eventName") ?: anno.resolveEventName(service, serviceName)
        val additionalKeys = anno.stringsField("additionalKeys")
        return KMessage(serviceName, eventName, keys, additionalKeys, contextual, contextNullable)
    }

    private fun kParam(par: KSValueParameter) =
        par.type.resolve().let { ksType ->
            KParam(
                par.name?.asString() ?: throw IllegalStateException("Null name: $par"),
                par.type.element.toString(),
                ksType.isMarkedNullable
            )
        }

    private fun KSAnnotation.resolveEventName(service: KService, serviceName: String) =
        when {
            boolField("fullEventName") -> "${service.service}_${serviceName}"
            boolField("simpleEventName") -> serviceName
            else -> null
        }

    private fun verified(service: KService, decl: KSFunctionDeclaration) =
        decl.also {
            if (it.returnType != null && it.returnType?.toString() != "Unit") {
                throw IllegalStateException(
                    "Message methods must return ${Unit::class.java.simpleName}:" +
                            " ${service.qualifiedService}#${it.simpleName.asString()} returned ${it.returnType?.element}"
                )
            }
        }

    private fun writer(service: KService, className: String) =
        PrintWriter(codeGenerator.mediatorClassFile(service, className), true)
}

private val ANNOTATION_NAME = Message::class.java.name

private val ANNOTATION_SHORT_NAME = Message::class.java.simpleName
