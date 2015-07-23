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
package org.jetbrains.kotlin.ui.commands.findReferences

import org.eclipse.core.commands.AbstractHandler
import org.eclipse.core.commands.ExecutionEvent
import org.eclipse.jdt.core.JavaCore
import org.eclipse.ui.handlers.HandlerUtil
import org.jetbrains.kotlin.ui.editors.KotlinEditor
import org.eclipse.jdt.core.IJavaElement
import org.jetbrains.kotlin.psi.JetReferenceExpression
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.eclipse.jdt.ui.actions.FindReferencesAction
import org.jetbrains.kotlin.psi.JetElement
import org.eclipse.jface.text.ITextSelection
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.eclipse.core.resources.IFile
import org.jetbrains.kotlin.core.references.getReferenceExpression
import org.eclipse.ui.ISources
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.core.model.findLightJavaElement
import org.jetbrains.kotlin.core.references.KotlinReference
import org.jetbrains.kotlin.core.references.createReference
import org.jetbrains.kotlin.core.references.resolveToLightElements
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.ui.actions.FindReferencesInProjectAction

public class KotlinFindReferencesInProjectHandler : KotlinFindReferencesHandler() {
    override fun findReferences(element: IJavaElement, editor: KotlinEditor) {
        FindReferencesInProjectAction(editor.getSite()).run(element)
    }
}

public class KotlinFindReferencesInWorkspaceHandler : KotlinFindReferencesHandler() {
    override fun findReferences(element: IJavaElement, editor: KotlinEditor) {
        FindReferencesAction(editor.getSite()).run(element)
    }
}

abstract class KotlinFindReferencesHandler : AbstractHandler() {
    override public fun execute(event: ExecutionEvent): Any? {
        val file = getFile(event)!!
        val javaProject = JavaCore.create(file.getProject())
        val kotlinEditor = HandlerUtil.getActiveEditor(event) as KotlinEditor
        
        val jetElement = getJetElement(event)
        if (jetElement == null) return null
        
        val referenceExpression = getReferenceExpression(jetElement)
        val targetLightElement = 
            if (referenceExpression != null) {
	            val reference = createReference(referenceExpression)
	            getOriginLightElement(reference, file, javaProject)
	        } else {
	            findLightJavaElement(jetElement, javaProject)
	        }
        
        if (targetLightElement == null) return null
        
        findReferences(targetLightElement, kotlinEditor)
        
        return null
    }
    
    override fun setEnabled(evaluationContext: Any) {
        val editorObject = HandlerUtil.getVariable(evaluationContext, ISources.ACTIVE_EDITOR_NAME)
        setBaseEnabled(editorObject is KotlinEditor)
    }
    
    abstract fun findReferences(element: IJavaElement, editor: KotlinEditor)
    
    private fun getOriginLightElement(reference: KotlinReference, file: IFile, javaProject: IJavaProject): IJavaElement? {
        val analysisResult = KotlinAnalyzer.analyzeFile(javaProject, KotlinPsiManager.INSTANCE.getParsedFile(file))
        return reference.resolveToLightElements(analysisResult.bindingContext, javaProject).firstOrNull()
    }
    
    private fun getJetElement(event: ExecutionEvent): JetElement? {
        val activeEditor = HandlerUtil.getActiveEditor(event) as KotlinEditor
        val selection = HandlerUtil.getActiveMenuSelection(event) as ITextSelection
        
        val psiElement = EditorUtil.getPsiElement(activeEditor, selection.getOffset())
        if (psiElement != null) {
            return PsiTreeUtil.getNonStrictParentOfType(psiElement, javaClass<JetElement>())
        }
        
        return null
    }
    
    private fun getFile(event: ExecutionEvent): IFile? {
        val activeEditor = HandlerUtil.getActiveEditor(event)
        return EditorUtil.getFile(activeEditor)
    }
}