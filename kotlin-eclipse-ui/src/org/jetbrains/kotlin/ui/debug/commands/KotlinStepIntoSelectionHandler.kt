/*******************************************************************************
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *******************************************************************************/
package org.jetbrains.kotlin.ui.debug.commands

import org.eclipse.core.commands.AbstractHandler
import org.eclipse.core.commands.ExecutionEvent
import org.eclipse.ui.handlers.HandlerUtil
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.eclipse.jface.text.ITextSelection
import org.eclipse.jdt.internal.debug.ui.EvaluationContextManager
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.jetbrains.kotlin.core.references.getReferenceExpression
import org.jetbrains.kotlin.core.references.resolveToSourceElements
import org.jetbrains.kotlin.core.references.createReferences
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.core.resolve.lang.java.resolver.EclipseJavaSourceElement
import org.jetbrains.kotlin.core.model.sourceElementsToLightElements
import org.eclipse.jdt.core.IJavaElement
import org.eclipse.jdt.debug.core.IJavaStackFrame
import org.eclipse.jdt.internal.debug.ui.actions.StepIntoSelectionHandler
import org.eclipse.jdt.debug.core.IJavaThread
import org.eclipse.jdt.core.IMethod
import org.jetbrains.kotlin.ui.debug.findTopmostType
import org.eclipse.jdt.internal.debug.ui.actions.StepIntoSelectionUtils
import org.eclipse.ui.IEditorPart
import org.eclipse.debug.core.model.IThread
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.eclipse.jdt.core.IType

class KotlinStepIntoSelectionHandler : AbstractHandler() {
    override fun execute(event: ExecutionEvent): Any? {
        val editor = HandlerUtil.getActiveEditor(event) as KotlinFileEditor
        val selection = editor.editorSite.selectionProvider.selection
        if (selection is ITextSelection) {
            stepIntoSelection(editor, selection)
        }
        
        return null
    }
}

private fun stepIntoSelection(editor: KotlinFileEditor, selection: ITextSelection) {
    val frame = EvaluationContextManager.getEvaluationContext(editor)
    if (frame == null || !frame.isSuspended) return
    
    val psiElement = EditorUtil.getPsiElement(editor, selection.offset) ?: return

    val expression = getReferenceExpression(psiElement) ?: return

    val sourceElements = createReferences(expression).resolveToSourceElements(expression.containingKtFile)
    val javaElements = sourceElementsToLightElements(sourceElements)
    if (javaElements.size > 1) {
        KotlinLogger.logWarning("There are more than one java element for $sourceElements")
        return
    }

    val method = when (val element = javaElements.first()) {
        is IMethod -> element
        is IType -> element.getMethod(element.elementName, emptyArray())
        else -> null
    } ?: return

    stepIntoElement(method, frame, selection, editor)
}

private fun stepIntoElement(method: IMethod, frame: IJavaStackFrame, selection: ITextSelection, editor: KotlinFileEditor) {
    if (selection.startLine + 1 == frame.lineNumber) {
        val handler = StepIntoSelectionHandler(frame.thread as IJavaThread, frame, method)
        handler.step()
    } else {
        val refMethod = StepIntoSelectionUtils::class.java.getDeclaredMethod(
                "runToLineBeforeStepIn",
                IEditorPart::class.java,
                String::class.java,
                ITextSelection::class.java,
                IThread::class.java,
                IMethod::class.java)
        refMethod.isAccessible = true
        refMethod.invoke(null, editor, frame.receivingTypeName, selection, frame.thread, method)
    }
}
