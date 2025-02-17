package org.jetbrains.kotlinx.jupyter.repl.impl

import org.jetbrains.kotlinx.jupyter.api.ExecutionCallback
import org.jetbrains.kotlinx.jupyter.api.ExecutionsProcessor
import org.jetbrains.kotlinx.jupyter.api.Notebook
import org.jetbrains.kotlinx.jupyter.codegen.ClassAnnotationsProcessor
import org.jetbrains.kotlinx.jupyter.codegen.FieldsProcessorInternal
import org.jetbrains.kotlinx.jupyter.codegen.FileAnnotationsProcessor
import org.jetbrains.kotlinx.jupyter.codegen.ResultsRenderersProcessor
import org.jetbrains.kotlinx.jupyter.codegen.TextRenderersProcessorWithPreventingRecursion
import org.jetbrains.kotlinx.jupyter.codegen.ThrowableRenderersProcessor
import org.jetbrains.kotlinx.jupyter.execution.ColorSchemeChangeCallbacksProcessor
import org.jetbrains.kotlinx.jupyter.execution.InterruptionCallbacksProcessor
import org.jetbrains.kotlinx.jupyter.libraries.LibrariesProcessor
import org.jetbrains.kotlinx.jupyter.libraries.LibrariesScanner
import org.jetbrains.kotlinx.jupyter.libraries.LibraryResourcesProcessor
import org.jetbrains.kotlinx.jupyter.magics.CompoundCodePreprocessor
import org.jetbrains.kotlinx.jupyter.repl.InternalEvaluator
import org.jetbrains.kotlinx.jupyter.repl.InternalVariablesMarkersProcessor

data class SharedReplContext(
    val classAnnotationsProcessor: ClassAnnotationsProcessor,
    val fileAnnotationsProcessor: FileAnnotationsProcessor,
    val fieldsProcessor: FieldsProcessorInternal,
    val renderersProcessor: ResultsRenderersProcessor,
    val textRenderersProcessor: TextRenderersProcessorWithPreventingRecursion,
    val throwableRenderersProcessor: ThrowableRenderersProcessor,
    val codePreprocessor: CompoundCodePreprocessor,
    val resourcesProcessor: LibraryResourcesProcessor,
    val librariesProcessor: LibrariesProcessor,
    val librariesScanner: LibrariesScanner,
    val notebook: Notebook,
    val beforeCellExecutionsProcessor: ExecutionsProcessor<ExecutionCallback<*>>,
    val shutdownExecutionsProcessor: ExecutionsProcessor<ExecutionCallback<*>>,
    val afterCellExecutionsProcessor: AfterCellExecutionsProcessor,
    val evaluator: InternalEvaluator,
    val baseHost: BaseKernelHost,
    val internalVariablesMarkersProcessor: InternalVariablesMarkersProcessor,
    val interruptionCallbacksProcessor: InterruptionCallbacksProcessor,
    val colorSchemeChangeCallbacksProcessor: ColorSchemeChangeCallbacksProcessor,
)
