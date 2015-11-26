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
import org.eclipse.jface.text.IDocument
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.ui.editors.annotations.DiagnosticAnnotationUtil

public class KotlinChangeReturnTypeQuickFix : KotlinQuickAssistProposal() {
    override fun apply(document: IDocument, psiElement: PsiElement) {
        val editor = getActiveEditor()
        if (editor == null) return
        
        val annotation = DiagnosticAnnotationUtil.INSTANCE.getAnnotationByOffset(editor, getStartOffset(psiElement, editor))
        val diagnostic = annotation?.getDiagnostic()
        if (diagnostic == null) return
    }
    
    override fun getDisplayString(): String = "Change return type"
    
    override fun isApplicable(psiElement: PsiElement): Boolean {
        val editor = getActiveEditor()
        if (editor == null) return false
        
        return isDiagnosticAnnotationActiveForElement(Errors.CONSTANT_EXPECTED_TYPE_MISMATCH, editor)
    }
}