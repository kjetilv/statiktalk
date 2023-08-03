package com.github.kjetilv.statiktalk.processor

import com.github.kjetilv.statiktalk.api.Message
import com.github.kjetilv.statiktalk.processor.ksp.eventName
import com.github.kjetilv.statiktalk.processor.ksp.findAnno
import com.github.kjetilv.statiktalk.processor.ksp.syntheticEventName
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*

internal object Messages {

    fun Resolver.serviceMessages(contextType: KSName) = try {
        getSymbolsWithAnnotation(ANNOTATION_NAME).let { symbols ->
            typeAnnotations(symbols, contextType) + functionAnnotations(symbols, contextType)
        }
    } catch (e: Exception) {
        throw IllegalStateException("Failed to process messages", e)
    }

    private fun functionAnnotations(symbols: Sequence<KSAnnotated>, contextType: KSName) =
        symbols.mapNotNull { it as? KSFunctionDeclaration }
            .groupBy { declaringClass(it) }
            .mapKeys { (classDecl, _) -> kService(classDecl) }
            .mapValues { (service, funDecls) ->
                verified(funDecls).map { functionDecl ->
                    verified(service, functionDecl).let { verifiedFunctionDecl ->
                        kMessage(service, verifiedFunctionDecl, contextType, verifiedFunctionDecl.findAnno(ANNOTATION_SHORT_NAME))
                    }
                }
            }

    private fun typeAnnotations(symbols: Sequence<KSAnnotated>, contextType: KSName) =
        symbols.mapNotNull { it as? KSClassDeclaration }
            .associateBy(::kService)
            .mapValues { (service, classDecl) ->
                classDecl.getDeclaredFunctions().map { functionDecl ->
                    kMessage(service, verified(service, functionDecl), contextType, classDecl.findAnno(ANNOTATION_SHORT_NAME))
                }.toList()
            }

    private val ANNOTATION_NAME = Message::class.java.name

    private val ANNOTATION_SHORT_NAME = Message::class.java.simpleName

    private fun declaringClass(decl: KSFunctionDeclaration) =
        (decl.parentDeclaration as? KSClassDeclaration)
            ?.takeIf { it.classKind == ClassKind.INTERFACE }
            ?: throw IllegalStateException("An interface is needed to hold $decl")

    private fun kService(decl: KSClassDeclaration) =
        KService(
            decl.packageName.asString(),
            "${decl.packageName.asString()}.generated",
            decl.simpleName.asString(),
            decl.containingFile
        )

    private fun kMessage(service: KService, decl: KSFunctionDeclaration, contextType: KSName, anno: KSAnnotation): KMessage {
        val params = decl.parameters
        val lastParam = params.lastOrNull()
        val contextArg = if (isContextArg(lastParam, contextType)) lastParam?.name?.asString() else null
        val contextNullable = contextArg == null || (lastParam?.type?.resolve()?.isMarkedNullable ?: false)
        val serviceName = decl.simpleName.asString()
        val keys = (if (contextArg == null) params else params.dropLast(1)).map(Messages::kParam)
        val eventName = anno.eventName ?: anno.syntheticEventName(service, serviceName, keys)
        return KMessage(serviceName, eventName, keys, contextArg, contextNullable)
    }

    private fun isContextArg(lastParam: KSValueParameter?, contextType: KSName) =
        lastParam?.type?.toString()?.isContext(contextType) ?: false

    private fun String.isContext(type: KSName) = this in setOf(type.getShortName(), this == type.asString())

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
