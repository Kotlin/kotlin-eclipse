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
package org.jetbrains.kotlin.ui.editors

import org.eclipse.jdt.ui.actions.SelectionDispatchAction
import org.eclipse.jdt.internal.ui.actions.ActionMessages
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds
import org.eclipse.jface.text.ITextSelection
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.core.model.KotlinAnalysisFileCache
import org.eclipse.jdt.core.IJavaProject
import org.jetbrains.kotlin.psi.KtFile

public class KotlinOpenSuperImplementationAction(val editor: KotlinFileEditor) : SelectionDispatchAction(editor.site) {
    init {
        setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_SUPER_IMPLEMENTATION)
        setText(ActionMessages.OpenSuperImplementationAction_label)
        setDescription(ActionMessages.OpenSuperImplementationAction_description)
    }
    
    override fun run(selection: ITextSelection) {
        val psiElement = EditorUtil.getPsiElement(editor, selection.offset)
        if (psiElement == null) return
        
        val declaration = PsiTreeUtil.getParentOfType(psiElement, 
                KtNamedFunction::class.java,
                KtClass::class.java,
                KtProperty::class.java,
                KtObjectDeclaration::class.java)
        if (declaration == null) return
        
        
    }
    
    private fun resolveToDescriptor(declaration: KtDeclaration, ktFile: KtFile, project: IJavaProject) {
        val context = KotlinAnalysisFileCache.getAnalysisResult(ktFile, project).analysisResult.bindingContext
    }
}