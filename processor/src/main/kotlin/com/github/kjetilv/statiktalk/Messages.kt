package com.github.kjetilv.statiktalk

import com.github.kjetilv.statiktalk.api.Message
import com.github.kjetilv.statiktalk.ksp.explicitEventName
import com.github.kjetilv.statiktalk.ksp.findAnno
import com.github.kjetilv.statiktalk.ksp.syntheticEventName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*

object Messages {

    internal fun Resolver.serviceMessages(contextType: KSName) =
        try {
            getSymbolsWithAnnotation(ANNOTATION_NAME)
                .mapNotNull { it as? KSFunctionDeclaration }
                .groupBy { declaringClass(it) }
                .mapKeys { (classDeclaration, _) ->
                    kService(classDeclaration)
                }
                .mapValues { (service, functionDeclarations) ->
                    verified(functionDeclarations).map {
                        kMessage(service, verified(service, it), contextType)
                    }
                }
        } catch (e: Exception) {
            throw IllegalStateException("Failed to process messages", e)
        }

    private val ANNOTATION_NAME = Message::class.java.name

    private val ANNOTATION_SHORT_NAME = Message::class.java.simpleName

    private fun declaringClass(decl: KSFunctionDeclaration) =
        (decl.parentDeclaration as? KSClassDeclaration)?.takeIf {
            it.classKind == ClassKind.INTERFACE
        } ?: throw IllegalStateException("An interface is needed to hold $decl")

    private fun kService(decl: KSClassDeclaration) =
        KService(
            decl.packageName.asString(),
            "${decl.packageName.asString()}.generated",
            decl.simpleName.asString(),
            decl.containingFile
        )

    private fun kMessage(service: KService, decl: KSFunctionDeclaration, contextType: KSName): KMessage {
        val anno = decl.findAnno(ANNOTATION_SHORT_NAME)
        val valueParameters = decl.parameters
        val lastParam = valueParameters.lastOrNull()
        val contextArg = if (isContextArg(lastParam, contextType)) lastParam?.name?.asString() else null
        val contextNullable = contextArg == null || (lastParam?.type?.resolve()?.isMarkedNullable ?: false)

        val serviceName = decl.simpleName.asString()
        val keys =
            valueParameters
                .let { if (contextArg != null) it.dropLast(1) else it }
                .map(::kParam)
        val eventName =
            anno.explicitEventName ?: anno.syntheticEventName(service, serviceName, keys)

        return KMessage(serviceName, eventName, keys, contextArg, contextNullable)
    }

    private fun isContextArg(
        lastParam: KSValueParameter?,
        contextType: KSName
    ) =
        lastParam?.type?.toString()
            ?.let { it == contextType.getShortName() || it == contextType.asString() }
            ?: false

    private fun kParam(par: KSValueParameter) =
        par.type.resolve().let { ksType ->
            KParam(
                par.name?.asString() ?: throw IllegalStateException("Null name: $par"),
                par.type.element.toString(),
                ksType.isMarkedNullable
            )
        }

    private fun verified(decls: List<KSFunctionDeclaration>) =
        decls.apply {
            groupBy { it.simpleName.asString() }
                .filterValues { it.size > 1 }
                .takeIf { it.isNotEmpty() }
                ?.keys
                ?.also {
                    throw IllegalStateException("Overloaded functions are not supported: ${it.joinToString(", ")}")
                }
        }

    private fun verified(service: KService, decl: KSFunctionDeclaration) =
        decl.apply {
            if (returnType != null && returnType?.toString() != "Unit") {
                throw IllegalStateException(
                    "Message methods must return ${Unit::class.java.simpleName}:" +
                            " ${service.qualifiedService}#${simpleName.asString()} returned ${returnType?.element}"
                )
            }
        }
}
