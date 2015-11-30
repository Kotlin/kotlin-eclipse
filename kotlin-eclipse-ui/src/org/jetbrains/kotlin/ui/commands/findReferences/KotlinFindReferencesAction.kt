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
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.eclipse.jdt.ui.actions.FindReferencesAction
import org.jetbrains.kotlin.psi.KtElement
import org.eclipse.jface.text.ITextSelection
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.eclipse.core.resources.IFile
import org.jetbrains.kotlin.core.references.getReferenceExpression
import org.eclipse.ui.ISources
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.core.model.toLightElements
import org.jetbrains.kotlin.core.references.KotlinReference
import org.jetbrains.kotlin.core.references.createReferences
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
import org.jetbrains.kotlin.core.references.resolveToSourceDeclaration
import org.eclipse.jdt.core.search.IJavaSearchConstants
import org.eclipse.jdt.ui.search.PatternQuerySpecification
import org.eclipse.jdt.core.search.IJavaSearchScope
import kotlin.properties.Delegates
import org.eclipse.jdt.ui.search.QuerySpecification
import org.eclipse.jdt.ui.search.ElementQuerySpecification
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.eclipse.jdt.ui.actions.SelectionDispatchAction
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds
import org.eclipse.jface.text.TextSelection
import org.eclipse.jdt.internal.ui.search.SearchMessages
import org.eclipse.jdt.internal.ui.JavaPluginImages
import org.eclipse.ui.PlatformUI
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.ui.search.getKotlinFiles

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
    
    override fun createScopeQuerySpecification(jetElement: KtElement): QuerySpecification {
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
    
    override fun createScopeQuerySpecification(jetElement: KtElement): QuerySpecification {
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
        val query = JavaSearchQuery(querySpecification)
        
        SearchUtil.runQueryInBackground(query)
    }
    
    abstract fun createScopeQuerySpecification(jetElement: KtElement): QuerySpecification
    
    private fun getFile(event: ExecutionEvent): IFile? {
        val activeEditor = HandlerUtil.getActiveEditor(event)
        return EditorUtil.getFile(activeEditor)
    }
}

fun createQuerySpecification(jetElement: KtElement, javaProject: IJavaProject, scope: IJavaSearchScope, 
        description: String): QuerySpecification {
    val sourceElements = jetElement.resolveToSourceDeclaration(javaProject)
    return KotlinJavaQuerySpecification(sourceElements, IJavaSearchConstants.REFERENCES, scope, description)
}