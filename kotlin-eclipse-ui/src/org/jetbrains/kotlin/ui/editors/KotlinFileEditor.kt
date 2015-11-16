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

public class KotlinFileEditor : CompilationUnitEditor(), KotlinEditor {
    private val colorManager: IColorManager = JavaColorManager()
    
    private val bracketInserter: KotlinBracketInserter = KotlinBracketInserter()
    
    private val kotlinOutlinePage by lazy { KotlinOutlinePage(this, kotlinReconcilingStrategy) }
    
    private val kotlinToggleBreakpointAdapter by lazy { KotlinToggleBreakpointAdapter() }
    
    private val kotlinRunToLineAdapter by lazy { KotlinRunToLineAdapter() }
    
    private val kotlinMarkOccurrences by lazy { KotlinMarkOccurrences() }
    
    private var kotlinSemanticHighlighter: KotlinSemanticHighlighter? = null
    
    private val kotlinReconcilingStrategy by lazy { KotlinReconcilingStrategy(this) }
    
    private val kotlinAnnotationReconciler by lazy { KotlinLineAnnotationsReconciler() }
    
    override public fun getAdapter(required: Class<*>): Any? {
        return when (required) {
            IContentOutlinePage::class.java -> kotlinOutlinePage
            IToggleBreakpointsTarget::class.java -> kotlinToggleBreakpointAdapter
            IRunToLineTarget::class.java -> kotlinRunToLineAdapter
            else -> super<CompilationUnitEditor>.getAdapter(required)
        }
    }
    
    override public fun createPartControl(parent: Composite) {
        setSourceViewerConfiguration(FileEditorConfiguration(colorManager, this, getPreferenceStore(), kotlinReconcilingStrategy))
        kotlinReconcilingStrategy.addListener(kotlinAnnotationReconciler)
        
        super<CompilationUnitEditor>.createPartControl(parent)
        
        val sourceViewer = getSourceViewer()
        if (sourceViewer is ITextViewerExtension) {
            bracketInserter.setSourceViewer(sourceViewer)
            bracketInserter.addBrackets('{', '}')
            sourceViewer.prependVerifyKeyListener(bracketInserter)
        }
    }
    
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
        
        setAction(KotlinOverrideMembersAction.ACTION_ID, KotlinOverrideMembersAction(this))
        
        setAction(KotlinFindReferencesInProjectAction.ACTION_ID, KotlinFindReferencesInProjectAction(this))
        
        setAction(KotlinFindReferencesInWorkspaceAction.ACTION_ID, KotlinFindReferencesInWorkspaceAction(this))
        
        setAction(KotlinRenameAction.ACTION_ID, KotlinRenameAction(this))
        
        setAction(KotlinExtractVariableAction.ACTION_ID, KotlinExtractVariableAction(this))
    }
    
    override fun installSemanticHighlighting() {
        val configuration = getSourceViewerConfiguration() as FileEditorConfiguration
        
        kotlinSemanticHighlighter = run {
            val scanner = configuration.getScanner()
            if (scanner != null) {
                val reconciler = Configuration.getKotlinPresentaionReconciler(scanner) 
                return@run KotlinSemanticHighlighter(getPreferenceStore(), colorManager, reconciler, this) 
            }
            
            null
        }
        
        if (kotlinSemanticHighlighter != null) {
            kotlinSemanticHighlighter!!.install()
            kotlinReconcilingStrategy.addListener(kotlinSemanticHighlighter!!)
        }
    }
    
    override public fun dispose() {
        colorManager.dispose()
        
        if (kotlinSemanticHighlighter != null) {
            kotlinReconcilingStrategy.removeListener(kotlinSemanticHighlighter!!)
            kotlinSemanticHighlighter!!.dispose()
        }
        
        kotlinReconcilingStrategy.removeListener(kotlinAnnotationReconciler)
        
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
    
    override fun installOccurrencesFinder(forceUpdate: Boolean) {
        getEditorSite().getPage().addPostSelectionListener(kotlinMarkOccurrences)
    }
    
    override fun uninstallOccurrencesFinder() {
        getEditorSite().getPage().removePostSelectionListener(kotlinMarkOccurrences)
    }
    
    public fun getFile(): IFile? = getEditorInput().getAdapter(IFile::class.java) as? IFile
    
    override val javaEditor: JavaEditor = this
    
    override val parsedFile: KtFile?
        get() = computeJetFile()
    
    override val javaProject: IJavaProject? by lazy {
        getFile()?.let { JavaCore.create(it.getProject()) }
    }
    
    override public fun isEditable(): Boolean = getFile() != null
    
    override val document: IDocument
        get() = getDocumentProvider().getDocument(getEditorInput())
    
    private fun computeJetFile(): KtFile? {
        val file = getFile()
        if (file != null) {
            return KotlinPsiManager.INSTANCE.getParsedFile(file)
        }
        
        if (javaProject == null) {
            return null
        }
        
        val environment = KotlinEnvironment.getEnvironment(javaProject!!)
        val ideaProject = environment.getProject()
        return KtPsiFactory(ideaProject).createFile(StringUtil.convertLineSeparators(document.get(), "\n"))
    }
}