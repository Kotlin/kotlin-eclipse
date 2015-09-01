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
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetPsiFactory
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

public class KotlinFileEditor : CompilationUnitEditor(), KotlinEditor {
    private val colorManager: IColorManager = JavaColorManager()
    
    private val bracketInserter: BracketInserter = BracketInserter()
    
    private val kotlinOutlinePage by lazy { KotlinOutlinePage(this) }
    
    private val kotlinToggleBreakpointAdapter by lazy { KotlinToggleBreakpointAdapter() }
    
    private val kotlinRunToLineAdapter by lazy { KotlinRunToLineAdapter() }
    
    override public fun getAdapter(required: Class<*>): Any? {
        return when (required) {
            javaClass<IContentOutlinePage>() -> kotlinOutlinePage
            javaClass<IToggleBreakpointsTarget>() -> kotlinToggleBreakpointAdapter
            javaClass<IRunToLineTarget>() -> kotlinRunToLineAdapter
            else -> super<CompilationUnitEditor>.getAdapter(required)
        }
    }
    
    override public fun createPartControl(parent: Composite) {
        setSourceViewerConfiguration(FileEditorConfiguration(colorManager, this, getPreferenceStore()))
        
        super<CompilationUnitEditor>.createPartControl(parent)
        
        val sourceViewer = getSourceViewer()
        if (sourceViewer is ITextViewerExtension) {
            bracketInserter.setSourceViewer(sourceViewer)
            bracketInserter.addBrackets('{', '}')
            sourceViewer.prependVerifyKeyListener(bracketInserter)
        }
    }
    
    override protected fun isMarkingOccurrences(): Boolean = false
    
    override protected fun isTabsToSpacesConversionEnabled(): Boolean = IndenterUtil.isSpacesForTabs()
    
    override protected fun createActions() {
        super<CompilationUnitEditor>.createActions()
        
        setAction("QuickFormat", null)
        
        val formatAction = KotlinFormatAction(this)
        setAction(KotlinFormatAction.FORMAT_ACTION_TEXT, formatAction)
        markAsStateDependentAction(KotlinFormatAction.FORMAT_ACTION_TEXT, true)
        markAsSelectionDependentAction(KotlinFormatAction.FORMAT_ACTION_TEXT, true)
        PlatformUI.getWorkbench().getHelpSystem().setHelp(formatAction, IJavaHelpContextIds.FORMAT_ACTION)
        
        val selectionHistory = SelectionHistory(this)
        val historyAction = StructureSelectHistoryAction(this, selectionHistory)
        historyAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SELECT_LAST)
        setAction(KotlinSemanticSelectionAction.HISTORY, historyAction)
        selectionHistory.setHistoryAction(historyAction)
        
        setAction(KotlinOpenDeclarationAction.OPEN_EDITOR_TEXT, KotlinOpenDeclarationAction(this))
        
        setAction(KotlinSelectEnclosingAction.SELECT_ENCLOSING_TEXT, KotlinSelectEnclosingAction(this, selectionHistory))
        
        setAction(KotlinSelectPreviousAction.SELECT_PREVIOUS_TEXT, KotlinSelectPreviousAction(this, selectionHistory))
        
        setAction(KotlinSelectNextAction.SELECT_NEXT_TEXT, KotlinSelectNextAction(this, selectionHistory))
    }
    
    override public fun dispose() {
        colorManager.dispose()
        
        val sourceViewer = getSourceViewer()
        if (sourceViewer is ITextViewerExtension) {
            sourceViewer.removeVerifyKeyListener(bracketInserter)
        }
        
        super<CompilationUnitEditor>.dispose()
    }
    
    override public fun setSelection(element: IJavaElement) {
        KotlinOpenEditor.revealKotlinElement(this, element)
    }
    
    override protected fun initializeKeyBindingScopes() {
        setKeyBindingScopes(arrayOf<String>(
                "org.jetbrains.kotlin.eclipse.ui.kotlinEditorScope", 
                "org.eclipse.jdt.ui.javaEditorScope"))
    }
    
    public fun getFile(): IFile? = getEditorInput().getAdapter(javaClass<IFile>()) as? IFile
    
    override val javaEditor: JavaEditor = this
    
    override val parsedFile: JetFile?
        get() = computeJetFile()
    
    override val javaProject: IJavaProject? by lazy {
        getFile()?.let { JavaCore.create(it.getProject()) }
    }
    
    override public fun isEditable(): Boolean = getFile() != null
    
    override val document: IDocument
        get() = getDocumentProvider().getDocument(getEditorInput())
    
    private fun computeJetFile(): JetFile? {
        val file = getFile()
        if (file != null) {
            return KotlinPsiManager.INSTANCE.getParsedFile(file)
        }
        
        if (javaProject == null) {
            return null
        }
        
        val environment = KotlinEnvironment.getEnvironment(javaProject!!)
        val ideaProject = environment.getProject()
        return JetPsiFactory(ideaProject).createFile(StringUtil.convertLineSeparators(document.get(), "\n"))
    }
}