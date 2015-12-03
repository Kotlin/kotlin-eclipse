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
package org.jetbrains.kotlin.ui.editors.quickassist

import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IMarker
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.ITextSelection
import org.eclipse.jface.viewers.ISelection
import org.eclipse.ui.IWorkbenchWindow
import org.eclipse.ui.PlatformUI
import org.eclipse.ui.texteditor.AbstractTextEditor
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.jetbrains.kotlin.ui.editors.annotations.DiagnosticAnnotation
import org.jetbrains.kotlin.ui.editors.annotations.DiagnosticAnnotationUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace

abstract class KotlinQuickAssist {
    abstract fun isApplicable(psiElement: PsiElement): Boolean
    
    fun isApplicable(): Boolean {
        val element = getActiveElement()
        return if (element != null) isApplicable(element) else false
    }
    
    protected fun getActiveEditor(): KotlinFileEditor? {
        val workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
        return workbenchWindow?.getActivePage()?.getActiveEditor() as? KotlinFileEditor
    }
    
    protected fun getActiveElement(): PsiElement? {
        val editor = getActiveEditor()
        if (editor == null) return null
        
        val file = editor.getFile()
        if (file == null) {
            KotlinLogger.logError("Failed to retrieve IFile from editor " + editor, null)
            return null
        }
        
        val document = editor.document
        val ktFile = KotlinPsiManager.getKotlinFileIfExist(file, document.get())
        if (ktFile == null) return null
        
        val caretOffset = LineEndUtil.convertCrToDocumentOffset(document, getCaretOffset(editor))
        val activeElement = ktFile.findElementAt(caretOffset)
        return if (activeElement !is PsiWhiteSpace) activeElement else ktFile.findElementAt(caretOffset - 1)
    }
    
    protected fun getCaretOffset(activeEditor: KotlinFileEditor): Int {
        val selection = activeEditor.getSelectionProvider().getSelection()
        return if (selection is ITextSelection) selection.getOffset() else activeEditor.getViewer().getTextWidget().getCaretOffset()
    }
    
    protected fun getCaretOffsetInPSI(activeEditor: KotlinFileEditor, document: IDocument): Int {
        return LineEndUtil.convertCrToDocumentOffset(document, getCaretOffset(activeEditor))
    }
    
    fun isDiagnosticActiveForElement(diagnosticType: DiagnosticFactory<*>, attribute: String): Boolean {
        val editor = getActiveEditor()
        if (editor == null) return false
        
        val caretOffset = getCaretOffset(editor)
        val annotation = DiagnosticAnnotationUtil.INSTANCE.getAnnotationByOffset(editor, caretOffset)
        if (annotation != null) {
            val diagnostic = annotation.diagnostic
            return if (diagnostic != null) diagnostic.equals(diagnosticType) else false
        }
        
        val file = EditorUtil.getFile(editor)
        if (file == null) {
            KotlinLogger.logError("Failed to retrieve IFile from editor " + editor, null)
            return false
        }
        
        val marker = DiagnosticAnnotationUtil.INSTANCE.getMarkerByOffset(file, caretOffset)
        return if (marker != null) marker.getAttribute(attribute, false) else false
    }
}