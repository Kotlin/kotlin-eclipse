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
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.core.search.IJavaSearchConstants
import org.eclipse.jdt.core.search.IJavaSearchScope
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds
import org.eclipse.jdt.internal.ui.JavaPluginImages
import org.eclipse.jdt.internal.ui.search.JavaSearchQuery
import org.eclipse.jdt.internal.ui.search.JavaSearchScopeFactory
import org.eclipse.jdt.internal.ui.search.SearchMessages
import org.eclipse.jdt.internal.ui.search.SearchUtil
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds
import org.eclipse.jdt.ui.actions.SelectionDispatchAction
import org.eclipse.jdt.ui.search.QuerySpecification
import org.eclipse.jface.text.ITextSelection
import org.eclipse.ui.PlatformUI
import org.eclipse.ui.handlers.HandlerUtil
import org.jetbrains.kotlin.core.references.resolveToSourceDeclaration
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.ui.editors.KotlinCommonEditor
import kotlin.properties.Delegates

abstract class KotlinFindReferencesHandler : AbstractHandler() {
    override fun execute(event: ExecutionEvent): Any? {
        val editor = HandlerUtil.getActiveEditor(event)
        if (editor !is KotlinCommonEditor) return null

        getAction(editor).run(editor.viewer.selectionProvider.selection as ITextSelection)

        return null
    }

    abstract fun getAction(editor: KotlinCommonEditor): KotlinFindReferencesAction

}

class KotlinFindReferencesInProjectHandler : KotlinFindReferencesHandler() {
    override fun getAction(editor: KotlinCommonEditor): KotlinFindReferencesAction {
        return KotlinFindReferencesInProjectAction(editor)
    }

}

class KotlinFindReferencesInWorkspaceHandler : KotlinFindReferencesHandler() {
    override fun getAction(editor: KotlinCommonEditor): KotlinFindReferencesAction {
        return KotlinFindReferencesInWorkspaceAction(editor)
    }
}

class KotlinFindReferencesInProjectAction(editor: KotlinCommonEditor) : KotlinFindReferencesAction(editor) {
    init {
        actionDefinitionId = IJavaEditorActionDefinitionIds.SEARCH_REFERENCES_IN_PROJECT
        text = SearchMessages.Search_FindReferencesInProjectAction_label
        toolTipText = SearchMessages.Search_FindReferencesInProjectAction_tooltip
        imageDescriptor = JavaPluginImages.DESC_OBJS_SEARCH_REF
        PlatformUI.getWorkbench().helpSystem.setHelp(this, IJavaHelpContextIds.FIND_REFERENCES_IN_PROJECT_ACTION)
    }

    companion object {
        const val ACTION_ID = "SearchReferencesInProject"
    }

    override fun createScopeQuerySpecification(jetElement: KtElement): QuerySpecification {
        val factory = JavaSearchScopeFactory.getInstance()
        return createQuerySpecification(
            jetElement,
            factory.createJavaProjectSearchScope(javaProject, false),
            factory.getProjectScopeDescription(javaProject, false)
        )
    }
}

class KotlinFindReferencesInWorkspaceAction(editor: KotlinCommonEditor) : KotlinFindReferencesAction(editor) {
    init {
        actionDefinitionId = IJavaEditorActionDefinitionIds.SEARCH_REFERENCES_IN_WORKSPACE
        text = SearchMessages.Search_FindReferencesAction_label
        toolTipText = SearchMessages.Search_FindReferencesAction_tooltip
        imageDescriptor = JavaPluginImages.DESC_OBJS_SEARCH_REF
        PlatformUI.getWorkbench().helpSystem.setHelp(this, IJavaHelpContextIds.FIND_REFERENCES_IN_WORKSPACE_ACTION)
    }

    companion object {
        const val ACTION_ID = "SearchReferencesInWorkspace"
    }

    override fun createScopeQuerySpecification(jetElement: KtElement): QuerySpecification {
        val factory = JavaSearchScopeFactory.getInstance()
        return createQuerySpecification(
            jetElement,
            factory.createWorkspaceScope(false),
            factory.getWorkspaceScopeDescription(false)
        )
    }
}

abstract class KotlinFindReferencesAction(val editor: KotlinCommonEditor) : SelectionDispatchAction(editor.site) {
    var javaProject: IJavaProject by Delegates.notNull()

    override fun run(selection: ITextSelection) {
        val file = editor.eclipseFile ?: return

        javaProject = JavaCore.create(file.project)

        val jetElement = EditorUtil.getJetElement(editor, selection.offset) ?: return

        val querySpecification = createScopeQuerySpecification(jetElement)
        val query = JavaSearchQuery(querySpecification)

        SearchUtil.runQueryInBackground(query)
    }

    abstract fun createScopeQuerySpecification(jetElement: KtElement): QuerySpecification
}

fun createQuerySpecification(jetElement: KtElement, scope: IJavaSearchScope, description: String, limitTo: Int = IJavaSearchConstants.REFERENCES): QuerySpecification {
    val sourceElements = jetElement.resolveToSourceDeclaration()
    return KotlinJavaQuerySpecification(sourceElements, limitTo, scope, description)
}
