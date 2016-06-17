/*******************************************************************************
* Copyright 2000-2016 JetBrains s.r.o.
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
package org.jetbrains.kotlin.ui.editors.navigation

import org.jetbrains.kotlin.ui.editors.KotlinEditor
import org.eclipse.jdt.ui.actions.SelectionDispatchAction
import org.eclipse.jdt.internal.ui.actions.ActionMessages
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds
import org.eclipse.jface.text.ITextSelection
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.eclipse.jdt.core.IJavaProject
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.core.references.createReferences
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil

class KotlinOpenDeclarationAction(val editor: KotlinEditor) : SelectionDispatchAction(editor.javaEditor.site) {
    companion object {
        const val OPEN_EDITOR_TEXT = "OpenEditor"
    }
    
    init {
        setText(ActionMessages.OpenAction_declaration_label)
        setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_EDITOR)
    }
    
    override fun run(selection: ITextSelection) {
        val selectedExpression = EditorUtil.getReferenceExpression(editor, selection.offset)
        val javaProject = editor.javaProject
        if (selectedExpression == null || javaProject == null) return
        
        val data = getNavigationData(selectedExpression, javaProject)
        if (data == null) return
        
        gotoElement(data.sourceElement, data.descriptor, selectedExpression, editor, javaProject)
    }
    
    private fun getNavigationData(referenceExpression: KtReferenceExpression, javaProject: IJavaProject): NavigationData? {
        val context = KotlinAnalyzer.analyzeFile(referenceExpression.getContainingKtFile()).analysisResult.bindingContext
        return createReferences(referenceExpression)
                .asSequence()
                .flatMap { it.getTargetDescriptors(context).asSequence() }
                .mapNotNull { descriptor ->
                    val elementWithSource = getElementWithSource(descriptor, javaProject.project)
                    if (elementWithSource != null) NavigationData(elementWithSource, descriptor) else null
                }
                .firstOrNull()
    }
    
    private data class NavigationData(val sourceElement: SourceElement, val descriptor: DeclarationDescriptor)
}