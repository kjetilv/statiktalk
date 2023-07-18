package com.github.kjetilv.statiktalk

import com.github.kjetilv.statiktalk.api.Context
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSName
import java.io.PrintWriter
import java.util.concurrent.atomic.AtomicReference

class Processor(private val codeGenerator: CodeGenerator) : SymbolProcessor {

    override fun process(resolver: Resolver) =
        emptyList<KSAnnotated>().also {
            contextType(resolver).let { contextType ->
                resolver.writeFiles(contextType)
            }
        }

    private fun Resolver.writeFiles(contextType: KSName) {
        functionMap(this, contextType).forEach { (service, messages) ->
            writeFiles(service, messages)
        }
    }

    private fun writeFiles(service: KService, messages: List<KMessage>) {
        with(writer(service, "${service.service}SenderMediator")) {
            println(senderTemplate.source(service, messages))
        }
        with(writer(service, "${service.service}ReceiverMediator")) {
            println(receiverTemplate.source(service, messages))
        }
    }

    private fun writer(service: KService, className: String) =
        PrintWriter(codeGenerator.mediatorClassFile(service, className), true)

    companion object {

        private val contextClassName = AtomicReference<KSName>()

        fun contextType(resolver: Resolver): KSName = contextClassName.updateAndGet { resolved: KSName? ->
            resolved ?: resolveWith(resolver)
        }

        private fun resolveWith(resolver: Resolver) =
            resolver.getClassDeclarationByName(CONTEXT_CLASS)?.qualifiedName
                ?: throw IllegalStateException("Could not resolve context type")

        private val CONTEXT_CLASS = Context::class.java.name
    }
}
