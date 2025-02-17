package org.jetbrains.kotlinx.jupyter.api

import java.lang.reflect.Field
import kotlin.reflect.KClass

fun interface TextRenderer {
    fun render(processor: TextRenderersProcessor, value: Any?): String?
}

class TextRendererWithDescription(private val description: String, action: TextRenderer) : TextRenderer by action {
    override fun toString(): String {
        return "Text renderer: $description"
    }
}

data class TextRendererWithPriority(
    val renderer: TextRenderer,
    val priority: Int = ProcessingPriority.DEFAULT,
)

private fun String.indent(indent: String = "    ", exceptFirst: Boolean = false): String {
    val str = this
    return buildString {
        val lines = str.lines()
        for ((i, l) in lines.withIndex()) {
            if (!exceptFirst || i != 0) append(indent)
            append(l)
            if (i != lines.lastIndex) append("\n")
        }
    }
}

object TextRenderers {
    val NULL = TextRendererWithDescription("renders null") { _, value ->
        if (value == null) "null" else null
    }

    val PRIMITIVES = TextRendererWithDescription("renders strings, booleans and numbers") { _, value ->
        when (value) {
            is String -> value
            is Number -> value.toString()
            is Boolean -> value.toString()
            else -> null
        }
    }

    val ITERABLES = TextRendererWithDescription("renders iterables, elements are rendered transitively") { processor, value ->
        if (value !is Iterable<*>) {
            null
        } else {
            renderIterable(
                processor,
                processor.render(value::class),
                value,
            )
        }
    }

    val MAPS = TextRendererWithDescription("renders maps, keys and values are rendered transitively") { processor, value ->
        if (value !is Map<*, *>) {
            null
        } else {
            renderMap(processor, processor.render(value::class), value)
        }
    }

    val CLASS = TextRendererWithDescription("renders KClass<*> objects") { _, value ->
        if (value !is KClass<*>) {
            null
        } else {
            value.simpleName
        }
    }

    val OBJECT = TextRendererWithDescription("renders any objects") { processor, value ->
        if (value == null) {
            null
        } else {
            val valueMap = buildObjectPropertiesMap(value)
            renderMap(
                processor,
                processor.render(value::class),
                valueMap,
                "=",
                openBracket = "(",
                closeBracket = ")",
            )
        }
    }

    val AVOID = TextRendererWithDescription("skips text rendering for some kinds of objects such as java.lang.Class") { _, value ->
        if (value == null) {
            null
        } else {
            val clazz = value::class
            val className = clazz.qualifiedName
            if (className?.startsWith("java.lang.") == true) value.toString()
            else null
        }
    }

    private fun renderIterable(
        processor: TextRenderersProcessor,
        title: String,
        iterable: Iterable<*>,
        separator: String = ",",
        multiline: Boolean = false,
        openBracket: String = "[",
        closeBracket: String = "]",
    ): String {
        return buildString {
            append("$title$openBracket")
            if (multiline) {
                append('\n')
            }
            val collection = iterable as? Collection<*>
            for ((i, el) in iterable.withIndex()) {
                processor.render(el)
                    .let { rel -> if (multiline) rel.indent() else rel }
                    .let { append(it) }
                if (collection == null || i < collection.size - 1) {
                    append(separator)
                    if (!multiline) append(' ')
                }
                if (multiline) append('\n')
            }
            append(closeBracket)
        }
    }

    private fun renderMap(
        processor: TextRenderersProcessor,
        title: String,
        map: Map<*, *>,
        arrowString: String = " => ",
        separator: String = ",",
        multiline: Boolean = false,
        openBracket: String = "{",
        closeBracket: String = "}",
    ): String {
        return buildString {
            append("$title$openBracket")
            if (multiline) {
                append('\n')
            }
            var i = 0
            for ((k, v) in map) {
                processor.render(k)
                    .let { rk -> if (multiline) rk.indent() else rk }
                    .let { append(it) }
                append(arrowString)
                processor.render(v)
                    .let { rv -> if (multiline) rv.indent(exceptFirst = true) else rv }
                    .let { append(it) }
                if (i < map.size - 1) {
                    append(separator)
                    if (!multiline) append(' ')
                }
                if (multiline) append('\n')
                ++i
            }
            append(closeBracket)
        }
    }

    private fun buildObjectPropertiesMap(obj: Any): Map<String, Any?> {
        val clazz: Class<*> = obj::class.java
        val fields: Array<Field> = clazz.declaredFields
        return hashMapOf<String, Any?>().apply {
            for (field in fields) {
                val name: String = field.name
                val value: Any = try {
                    field.isAccessible = true
                    field.get(obj)
                } catch (e: IllegalAccessException) {
                    "[exception thrown]"
                } catch (e: RuntimeException) {
                    "[inaccessible field]"
                }
                put(name, value)
            }
        }
    }
}

fun TextRenderersProcessor.registerDefaultRenderers() {
    register(TextRenderers.NULL, ProcessingPriority.DEFAULT)
    register(TextRenderers.MAPS, ProcessingPriority.DEFAULT)
    register(TextRenderers.ITERABLES, ProcessingPriority.LOW)
    register(TextRenderers.PRIMITIVES, ProcessingPriority.DEFAULT)
    register(TextRenderers.CLASS, ProcessingPriority.DEFAULT)
    register(TextRenderers.OBJECT, ProcessingPriority.LOWER)
    register(TextRenderers.AVOID, ProcessingPriority.HIGH)
}
