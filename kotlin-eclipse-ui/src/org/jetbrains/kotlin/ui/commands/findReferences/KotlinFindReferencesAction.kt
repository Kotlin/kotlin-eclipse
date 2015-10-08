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
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.eclipse.jdt.ui.actions.SelectionDispatchAction
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds
import org.eclipse.jface.text.TextSelection
import org.eclipse.jdt.internal.ui.search.SearchMessages
import org.eclipse.jdt.internal.ui.JavaPluginImages
import org.eclipse.ui.PlatformUI
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds
import org.jetbrains.kotlin.core.log.KotlinLogger

abstract class KotlinFindReferencesHandler : AbstractHandler() {
    override fun execute(event: ExecutionEvent): Any? {
        val editor = HandlerUtil.getActiveEditor(event)
        if (editor !is KotlinFileEditor) return null
        
        getAction(editor).run(editor.getViewer().getSelectionProvider().getSelection() as ITextSelection)
        
        return null
    }
    
    abstract fun getAction(editor: KotlinFileEditor): KotlinFindReferencesAction
    
}

class KotlinFindReferencesInProjectHandler : KotlinFindReferencesHandler() {
    override fun getAction(editor: KotlinFileEditor): KotlinFindReferencesAction {
        return KotlinFindReferencesInProjectAction(editor)
    }

}
class KotlinFindReferencesInWorkspaceHandler : KotlinFindReferencesHandler() {
    override fun getAction(editor: KotlinFileEditor): KotlinFindReferencesAction {
        return KotlinFindReferencesInWorkspaceAction(editor)
    }
}

public class KotlinFindReferencesInProjectAction(editor: KotlinFileEditor) : KotlinFindReferencesAction(editor) {
    init {
        setActionDefinitionId(IJavaEditorActionDefinitionIds.SEARCH_REFERENCES_IN_PROJECT)
        setText(SearchMessages.Search_FindReferencesInProjectAction_label)
        setToolTipText(SearchMessages.Search_FindReferencesInProjectAction_tooltip)
        setImageDescriptor(JavaPluginImages.DESC_OBJS_SEARCH_REF)
        PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.FIND_REFERENCES_IN_PROJECT_ACTION)
    }
    
    companion object {
        val ACTION_ID = "SearchReferencesInProject"
    }
    
    override fun createScopeQuerySpecification(jetElement: JetElement): QuerySpecification? {
        val factory = JavaSearchScopeFactory.getInstance()
        return createQuerySpecification(
                jetElement,
                javaProject,
                factory.createJavaProjectSearchScope(javaProject, false), 
                factory.getProjectScopeDescription(javaProject, false))
    }
}

public class KotlinFindReferencesInWorkspaceAction(editor: KotlinFileEditor) : KotlinFindReferencesAction(editor) {
    init {
        setActionDefinitionId(IJavaEditorActionDefinitionIds.SEARCH_REFERENCES_IN_WORKSPACE)
        setText(SearchMessages.Search_FindReferencesAction_label)
        setToolTipText(SearchMessages.Search_FindReferencesAction_tooltip)
        setImageDescriptor(JavaPluginImages.DESC_OBJS_SEARCH_REF)
        PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.FIND_REFERENCES_IN_WORKSPACE_ACTION)
    }
    
    companion object {
        val ACTION_ID = "SearchReferencesInWorkspace"
    }
    
    override fun createScopeQuerySpecification(jetElement: JetElement): QuerySpecification? {
        val factory = JavaSearchScopeFactory.getInstance()
        return createQuerySpecification(
                jetElement,
                javaProject,
                factory.createWorkspaceScope(false), 
                factory.getWorkspaceScopeDescription(false))
    }
}

abstract class KotlinFindReferencesAction(val editor: KotlinFileEditor) : SelectionDispatchAction(editor.getSite()) {
    var javaProject: IJavaProject by Delegates.notNull()
    
    override public fun run(selection: ITextSelection) {
        val file = editor.getFile()
        if (file == null) return
        
        javaProject = JavaCore.create(file.getProject())
        
        val jetElement = EditorUtil.getJetElement(editor, selection.getOffset())
        if (jetElement == null) return
        
        val querySpecification = createScopeQuerySpecification(jetElement)
        if (querySpecification == null) return
        
        val query = JavaSearchQuery(querySpecification)
        
        SearchUtil.runQueryInBackground(query)
    }
    
    abstract fun createScopeQuerySpecification(jetElement: JetElement): QuerySpecification?
    
    private fun getFile(event: ExecutionEvent): IFile? {
        val activeEditor = HandlerUtil.getActiveEditor(event)
        return EditorUtil.getFile(activeEditor)
    }
}

fun createQuerySpecification(jetElement: JetElement, javaProject: IJavaProject, scope: IJavaSearchScope, 
        description: String): QuerySpecification? {
    
    fun createFindReferencesQuery(elements: List<IJavaElement>): QuerySpecification {
        return when (elements.size()) {
            1 -> ElementQuerySpecification(elements[0], IJavaSearchConstants.REFERENCES, scope, description)
            else -> {
                val isJetElementIsDistinct = elements.all { it.getElementName() != jetElement.getName() }
                KotlinCompositeQuerySpecification(
                        elements, 
                        if (isJetElementIsDistinct) jetElement else null,
                        scope,
                        description)
            }
        }
    }
    
    fun createFindReferencesQuery(element: JetElement): KotlinQueryPatternSpecification {
        return KotlinQueryPatternSpecification(element, scope, description)
    }
    
    return when (jetElement) {
        is JetObjectDeclarationName -> {
            val objectDeclaration = PsiTreeUtil.getParentOfType(jetElement, JetObjectDeclaration::class.java)
            objectDeclaration?.let { createQuerySpecification(it, javaProject, scope, description) }
        }
        
        is JetDeclaration -> {
            val lightElements = jetElement.toLightElements(javaProject)
            if (lightElements.isNotEmpty()) {
                createFindReferencesQuery(lightElements)
            } else {
                // Element should present only in Kotlin as there is no corresponding light element
                createFindReferencesQuery(jetElement)
            }
        }
        
        else -> {
            // Try search usages by reference
            val referenceExpression = getReferenceExpression(jetElement)
            if (referenceExpression == null) return null
            
            val reference = createReference(referenceExpression)
            val sourceElements = reference.resolveToSourceElements()
            val lightElements = sourceElementsToLightElements(sourceElements, javaProject)
            if (lightElements.isNotEmpty()) {
                createFindReferencesQuery(lightElements)
            } else {
                if (sourceElements.size() > 1) {
                    KotlinLogger.logWarning("There are more than one elements for ${referenceExpression.getText()}")
                }
                
                val kotlinSourceElement = sourceElements[0] as KotlinSourceElement
                createFindReferencesQuery(kotlinSourceElement.psi)
            }
        } 
    }
}

// This pattern is using to run composite search which is described in KotlinQueryParticipant.
class KotlinCompositeQuerySpecification(
        val lightElements: List<IJavaElement>, 
        val jetElement: JetElement?, 
        searchScope: IJavaSearchScope, 
        description: String) : KotlinDummyQuerySpecification(searchScope, description)

// Using of this pattern assumes that elements presents only in Kotlin
class KotlinQueryPatternSpecification(
        val jetElement: JetElement, 
        searchScope: IJavaSearchScope, 
        description: String) : KotlinDummyQuerySpecification(searchScope, description)

// After passing this query specification to java, it will try to find some usages and to ensure that nothing will found
// before KotlinQueryParticipant here is using dummy element '------------'
open class KotlinDummyQuerySpecification(searchScope: IJavaSearchScope, description: String) : PatternQuerySpecification(
        "Kotlin Find References", 
        IJavaSearchConstants.CLASS, 
        true, 
        IJavaSearchConstants.REFERENCES, 
        searchScope, 
        description
)