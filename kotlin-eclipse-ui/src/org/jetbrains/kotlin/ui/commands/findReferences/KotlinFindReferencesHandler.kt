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
import org.jetbrains.kotlin.core.model.toLightElements
import org.jetbrains.kotlin.core.references.KotlinReference
import org.jetbrains.kotlin.core.references.createReference
import org.jetbrains.kotlin.core.references.resolveToLightElements
import org.jetbrains.kotlin.core.references.resolveToSourceElements
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.ui.actions.FindReferencesInProjectAction
import org.eclipse.jdt.internal.core.JavaModelManager
import org.eclipse.jdt.internal.ui.search.JavaSearchScopeFactory
import org.eclipse.jdt.internal.ui.search.JavaSearchQuery
import org.eclipse.jdt.internal.ui.search.SearchUtil
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.core.resolve.lang.java.resolver.EclipseJavaSourceElement
import org.jetbrains.kotlin.core.model.sourceElementsToLightElements
import org.eclipse.jdt.core.search.IJavaSearchConstants
import org.eclipse.jdt.ui.search.PatternQuerySpecification
import org.eclipse.jdt.core.search.IJavaSearchScope
import org.jetbrains.kotlin.psi.JetDeclaration
import kotlin.properties.Delegates
import org.eclipse.jdt.ui.search.QuerySpecification
import org.eclipse.jdt.ui.search.ElementQuerySpecification
import org.jetbrains.kotlin.psi.JetObjectDeclarationName
import org.jetbrains.kotlin.psi.JetObjectDeclaration

public class KotlinFindReferencesInProjectHandler : KotlinFindReferencesHandler() {
    override fun createScopeQuerySpecification(jetElement: JetElement): QuerySpecification? {
        val factory = JavaSearchScopeFactory.getInstance()
        return createQuerySpecification(
                jetElement,
                javaProject,
                factory.createJavaProjectSearchScope(javaProject, false), 
                factory.getProjectScopeDescription(javaProject, false))
    }
}

public class KotlinFindReferencesInWorkspaceHandler : KotlinFindReferencesHandler() {
    override fun createScopeQuerySpecification(jetElement: JetElement): QuerySpecification? {
        val factory = JavaSearchScopeFactory.getInstance()
        return createQuerySpecification(
                jetElement,
                javaProject,
                factory.createWorkspaceScope(false), 
                factory.getWorkspaceScopeDescription(false))
    }
}

abstract class KotlinFindReferencesHandler : AbstractHandler() {
    var javaProject: IJavaProject by Delegates.notNull()
    
    override public fun execute(event: ExecutionEvent): Any? {
        val file = getFile(event)!!
        javaProject = JavaCore.create(file.getProject())
        
        val jetElement = getJetElement(event)
        if (jetElement == null) return null
        
        val querySpecification = createScopeQuerySpecification(jetElement)
        if (querySpecification == null) return null
        
        val query = JavaSearchQuery(querySpecification)
        
        SearchUtil.runQueryInBackground(query)
        
        return null
    }
    
    override fun setEnabled(evaluationContext: Any) {
        val editorObject = HandlerUtil.getVariable(evaluationContext, ISources.ACTIVE_EDITOR_NAME)
        setBaseEnabled(editorObject is KotlinEditor)
    }
    
    abstract fun createScopeQuerySpecification(jetElement: JetElement): QuerySpecification?
    
    private fun getJetElement(event: ExecutionEvent): JetElement? {
        val activeEditor = HandlerUtil.getActiveEditor(event) as KotlinEditor
        val selection = activeEditor.getSelectionProvider().getSelection() as? ITextSelection
        if (selection == null) return null
        
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

fun createQuerySpecification(jetElement: JetElement, javaProject: IJavaProject, scope: IJavaSearchScope, 
        description: String): QuerySpecification? {
    if (jetElement is JetObjectDeclarationName) {
        val objectDeclaration = PsiTreeUtil.getParentOfType(jetElement, javaClass<JetObjectDeclaration>())
        return objectDeclaration?.let { createQuerySpecification(it, javaProject, scope, description) }
    }
    
    fun createFindReferencesQuery(elements: List<IJavaElement>): QuerySpecification {
        return when (elements.size()) {
            1 -> ElementQuerySpecification(elements[0], IJavaSearchConstants.REFERENCES, scope, description)
            else -> KotlinLightElementsQuerySpecification(elements, scope, description)
        }
    }
    
    fun createFindReferencesQuery(elements: List<JetElement>): KotlinQueryPatternSpecification {
        return KotlinQueryPatternSpecification(elements, scope, description)
    }
    
    return if (jetElement is JetDeclaration) {
        val lightElements = jetElement.toLightElements(javaProject)
        if (lightElements.isNotEmpty()) {
            createFindReferencesQuery(lightElements)
        } else {
//          Element should present only in Kotlin as there is no corresponding light element
            createFindReferencesQuery(listOf(jetElement))
        }
    } else {
        // Try search usages by reference
        val referenceExpression = getReferenceExpression(jetElement)
        if (referenceExpression == null) return null
        
        val reference = createReference(referenceExpression)
        val sourceElements = reference.resolveToSourceElements()
        val lightElements = sourceElementsToLightElements(sourceElements, javaProject)
        if (lightElements.isNotEmpty()) {
            createFindReferencesQuery(lightElements)
        } else {
            createFindReferencesQuery(
                    sourceElements
                        .filterIsInstance(javaClass<KotlinSourceElement>())
                        .map { it.psi })
        }
    }
}

class KotlinLightElementsQuerySpecification(val lightElements: List<IJavaElement>, searchScope: IJavaSearchScope, description: String) :
        PatternQuerySpecification(
            "$$",
            IJavaSearchConstants.CLASS,
            true,
            IJavaSearchConstants.REFERENCES,
            searchScope,
            description)

class KotlinQueryPatternSpecification(val jetElements: List<JetElement>, searchScope: IJavaSearchScope, description: String) : 
        PatternQuerySpecification(
            jetElements.first().getName(), 
            IJavaSearchConstants.CLASS, 
            true, 
            IJavaSearchConstants.REFERENCES, 
            searchScope, 
            description)