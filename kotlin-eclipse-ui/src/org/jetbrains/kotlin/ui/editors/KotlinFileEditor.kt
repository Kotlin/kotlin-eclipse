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

import org.eclipse.core.resources.IFile
import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget
import org.eclipse.jdt.core.IJavaElement
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor
import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.SelectionHistory
import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.StructureSelectHistoryAction
import org.eclipse.jdt.internal.ui.text.JavaColorManager
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds
import org.eclipse.jdt.ui.text.IColorManager
import org.eclipse.jface.action.IAction
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.ITextViewerExtension
import org.eclipse.jface.text.source.ISourceViewer
import org.eclipse.swt.widgets.Composite
import org.eclipse.ui.PlatformUI
import org.eclipse.ui.views.contentoutline.IContentOutlinePage
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.model.KotlinEnvironment
import org.jetbrains.kotlin.eclipse.ui.utils.IndenterUtil
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.ui.debug.KotlinToggleBreakpointAdapter
import org.jetbrains.kotlin.ui.editors.outline.KotlinOutlinePage
import org.jetbrains.kotlin.ui.editors.selection.KotlinSelectEnclosingAction
import org.jetbrains.kotlin.ui.editors.selection.KotlinSelectNextAction
import org.jetbrains.kotlin.ui.editors.selection.KotlinSelectPreviousAction
import org.jetbrains.kotlin.ui.editors.selection.KotlinSemanticSelectionAction
import org.jetbrains.kotlin.ui.navigation.KotlinOpenEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.ui.debug.KotlinRunToLineAdapter
import org.eclipse.debug.ui.actions.IRunToLineTarget
import org.jetbrains.kotlin.ui.overrideImplement.KotlinOverrideMembersAction
import org.jetbrains.kotlin.ui.commands.findReferences.KotlinFindReferencesInProjectAction
import org.jetbrains.kotlin.ui.commands.findReferences.KotlinFindReferencesInWorkspaceAction
import org.jetbrains.kotlin.ui.refactorings.rename.KotlinRenameAction
import org.jetbrains.kotlin.ui.editors.occurrences.KotlinMarkOccurrences
import org.jetbrains.kotlin.ui.refactorings.extract.KotlinExtractVariableAction
import org.jetbrains.kotlin.ui.editors.highlighting.KotlinSemanticHighlighter
import org.jetbrains.kotlin.ui.editors.KotlinReconcilingStrategy
import org.jetbrains.kotlin.ui.editors.annotations.KotlinLineAnnotationsReconciler
import org.eclipse.jface.text.source.SourceViewerConfiguration
import org.jetbrains.kotlin.ui.editors.navigation.KotlinOpenDeclarationAction
import org.jetbrains.kotlin.ui.editors.navigation.KotlinOpenSuperImplementationAction
import org.jetbrains.kotlin.ui.editors.organizeImports.KotlinOrganizeImportsAction
import org.jetbrains.kotlin.ui.editors.navigation.StringInput

open class KotlinFileEditor : KotlinCommonEditor() {
    override val isScript: Boolean
        get() = false
    
    override val parsedFile: KtFile?
        get() = computeJetFile()
    
    override val javaProject: IJavaProject? by lazy {
        eclipseFile?.let { JavaCore.create(it.getProject()) }
    }
    
    override val document: IDocument
        get() = getDocumentProvider().getDocument(getEditorInput())
    
    private fun computeJetFile(): KtFile? {
        val file = eclipseFile
        if (file != null && file.exists()) {
            return KotlinPsiManager.getKotlinParsedFile(file) // File might be not under the source root
        }
        
        if (javaProject == null) {
            return null
        }
        
        val environment = KotlinEnvironment.getEnvironment(javaProject!!.project)
        val ideaProject = environment.project
        return KtPsiFactory(ideaProject).createFile(StringUtil.convertLineSeparators(document.get(), "\n"))
    }
}

class KotlinExternalSealedFileEditor : KotlinFileEditor() {
    companion object {
        const val EDITOR_ID = "org.jetbrains.kotlin.ui.editors.KotlinExternalSealedFileEditor"
    }
    
    override val parsedFile: KtFile?
        get() = (getEditorInput() as StringInput).getKtFile()
}