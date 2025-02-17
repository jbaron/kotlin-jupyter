package org.jetbrains.kotlinx.jupyter.api

import org.jetbrains.kotlinx.jupyter.api.libraries.ExecutionHost

/**
 * [RenderersProcessor] is responsible for rendering objects.
 * You may use it to render values exactly like notebook renders results,
 * and also register new renderers in runtime.
 */
interface RenderersProcessor {
    /**
     * Renders [value] in context of this execution [host]
     */
    fun renderValue(host: ExecutionHost, value: Any?): Any?

    /**
     * Adds new [renderer] for this notebook.
     * Don't turn on the optimizations for [PrecompiledRendererTypeHandler]
     */
    fun registerWithoutOptimizing(renderer: RendererHandler)

    fun registerWithoutOptimizing(renderer: RendererHandler, priority: Int)

    fun unregister(renderer: RendererHandler)

    fun registeredRenderers(): List<RendererHandlerWithPriority>
}
