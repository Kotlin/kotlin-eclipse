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

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.ITextSelection
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil
import org.jetbrains.kotlin.ui.editors.KotlinEditor

abstract class KotlinQuickAssist(val editor: KotlinEditor) {
    
    abstract fun isApplicable(psiElement: PsiElement): Boolean
    
    fun isApplicable(): Boolean {
        val element = getActiveElement()
        return if (element != null) isApplicable(element) else false
    }
    
    protected fun getActiveElement(): PsiElement? {
        val file = editor.eclipseFile
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
    
    protected fun getCaretOffset(activeEditor: KotlinEditor): Int {
        val selection = activeEditor.javaEditor.getSelectionProvider().getSelection()
        return if (selection is ITextSelection)
            selection.getOffset()
        else
            activeEditor.javaEditor.getViewer().getTextWidget().getCaretOffset()
    }
    
    protected fun getCaretOffsetInPSI(activeEditor: KotlinEditor, document: IDocument): Int {
        return LineEndUtil.convertCrToDocumentOffset(document, getCaretOffset(activeEditor))
    }
}