package org.jetbrains.kotlinx.jupyter.api

import kotlinx.serialization.Serializable
import org.jetbrains.kotlinx.jupyter.api.libraries.ExecutionHost
import org.jetbrains.kotlinx.jupyter.api.libraries.VariablesSubstitutionAware
import org.jetbrains.kotlinx.jupyter.util.TypeHandlerCodeExecutionSerializer
import org.jetbrains.kotlinx.jupyter.util.isSubclassOfCatching
import kotlin.reflect.KClass

/**
 * Execution interface for type handlers
 */
fun interface ResultHandlerExecution : VariablesSubstitutionAware<ResultHandlerExecution> {
    fun execute(host: ExecutionHost, result: FieldValue): FieldValue

    override fun replaceVariables(mapping: Map<String, String>): ResultHandlerExecution = this
}

data class FieldValue(val value: Any?, val name: String?)

data class RendererHandlerWithPriority(
    val renderer: RendererHandler,
    val priority: Int = ProcessingPriority.DEFAULT,
)

/**
 * Execution represented by code snippet.
 * This snippet should return the value.
 */
@Serializable(TypeHandlerCodeExecutionSerializer::class)
class ResultHandlerCodeExecution(val code: Code) : ResultHandlerExecution {
    override fun execute(host: ExecutionHost, result: FieldValue): FieldValue {
        val argTemplate = "\$it"
        val execCode = if (argTemplate in code) {
            val resName = result.name ?: run {
                val newName = "___myRes"
                host.execute {
                    declare(newName to result.value)
                }
                newName
            }
            code.replace(argTemplate, resName)
        } else code

        return host.execute {
            execute(execCode)
        }
    }

    override fun replaceVariables(mapping: Map<String, String>): ResultHandlerCodeExecution {
        return ResultHandlerCodeExecution(org.jetbrains.kotlinx.jupyter.util.replaceVariables(code, mapping))
    }
}

/**
 * [RendererHandler] renders results for which [accepts] returns `true`
 */
interface RendererHandler : VariablesSubstitutionAware<RendererHandler> {
    /**
     * Returns true if this renderer accepts [value], false otherwise
     */
    fun accepts(value: Any?): Boolean

    /**
     * Execution to handle result.
     * Should not throw if [accepts] returns true
     */
    val execution: ResultHandlerExecution
}

/**
 * [RendererTypeHandler] handles results for which runtime types [acceptsType] returns `true`
 */
interface RendererTypeHandler : RendererHandler {
    fun acceptsType(type: KClass<*>): Boolean

    override fun accepts(value: Any?): Boolean {
        return if (value == null) false else acceptsType(value::class)
    }
}

/**
 * Precompiled renderer type handler. Override this interface if
 * you want type rendering to be optimized.
 */
interface PrecompiledRendererTypeHandler : RendererTypeHandler {
    /**
     * `true` if this type handler may be precompiled
     */
    val mayBePrecompiled: Boolean

    /**
     * Returns method code for rendering
     *
     * @param methodName Precompiled method name
     * @param paramName Name for result value parameter
     * @return Method code if renderer may be precompiled, null otherwise
     */
    fun precompile(methodName: String, paramName: String): Code?
}

/**
 * Simple implementation for [RendererTypeHandler].
 * Renders any type by default
 */
open class AlwaysRendererTypeHandler(override val execution: ResultHandlerExecution) : RendererTypeHandler {
    override fun acceptsType(type: KClass<*>): Boolean = true
    override fun replaceVariables(mapping: Map<String, String>) = this

    override fun toString(): String {
        return "Renderer of any type${execution.asTextSuffix()}"
    }
}

/**
 * Serializable version of type handler.
 * Renders only classes which exactly match [className] by FQN.
 * Accepts only [ResultHandlerCodeExecution] because it's the only one that
 * may be correctly serialized.
 */
@Serializable
class ExactRendererTypeHandler(val className: TypeName, override val execution: ResultHandlerCodeExecution) : RendererTypeHandler {
    override fun acceptsType(type: KClass<*>): Boolean {
        return className == type.java.canonicalName
    }

    override fun replaceVariables(mapping: Map<String, String>): RendererTypeHandler {
        return ExactRendererTypeHandler(className, execution.replaceVariables(mapping))
    }

    override fun toString(): String {
        return "Exact renderer of $className${execution.asTextSuffix()}"
    }
}

/**
 * Renders any object of [superType] (including subtypes).
 * If [execution] is [ResultHandlerCodeExecution], this renderer may be
 * optimized by pre-compilation (unlike [ExactRendererTypeHandler]).
 */
class SubtypeRendererTypeHandler(private val superType: KClass<*>, override val execution: ResultHandlerExecution) : PrecompiledRendererTypeHandler {
    override val mayBePrecompiled: Boolean
        get() = execution is ResultHandlerCodeExecution

    override fun precompile(methodName: String, paramName: String): Code? {
        if (execution !is ResultHandlerCodeExecution) return null

        val typeParamsString = superType.typeParameters.run {
            if (isEmpty()) {
                ""
            } else {
                joinToString(", ", "<", ">") { "*" }
            }
        }
        val typeDef = superType.qualifiedName!! + typeParamsString
        val methodBody = org.jetbrains.kotlinx.jupyter.util.replaceVariables(execution.code, mapOf("it" to paramName))

        return "fun $methodName($paramName: $typeDef): Any? = $methodBody"
    }

    override fun acceptsType(type: KClass<*>): Boolean {
        return type.isSubclassOfCatching(superType)
    }

    override fun replaceVariables(mapping: Map<String, String>): SubtypeRendererTypeHandler {
        return SubtypeRendererTypeHandler(superType, execution.replaceVariables(mapping))
    }

    override fun toString(): String {
        return "Renderer of subtypes of $superType${execution.asTextSuffix()}"
    }
}

inline fun <T : Any> createRenderer(kClass: KClass<T>, crossinline renderAction: (T) -> Any?): RendererTypeHandler {
    return SubtypeRendererTypeHandler(kClass) { _, result ->
        @Suppress("UNCHECKED_CAST")
        FieldValue(renderAction(result.value as T), null)
    }
}

inline fun <reified T : Any> createRenderer(crossinline renderAction: (T) -> Any?): RendererTypeHandler {
    return createRenderer(T::class, renderAction)
}

private fun ResultHandlerExecution.asTextSuffix(): String {
    return (this as? ResultHandlerCodeExecution)
        ?.let { " with execution=[$code]" }
        .orEmpty()
}
