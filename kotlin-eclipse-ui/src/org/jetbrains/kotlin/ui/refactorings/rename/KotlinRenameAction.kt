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
package org.jetbrains.kotlin.ui.refactorings.rename

import org.eclipse.core.commands.operations.IUndoableOperation
import org.eclipse.core.commands.operations.OperationHistoryFactory
import org.eclipse.core.resources.IResource
import org.eclipse.core.runtime.NullProgressMonitor
import org.eclipse.jdt.core.IJavaElement
import org.eclipse.jdt.core.IMethod
import org.eclipse.jdt.core.IType
import org.eclipse.jdt.internal.ui.javaeditor.EditorHighlightingSynchronizer
import org.eclipse.jdt.internal.ui.refactoring.RefactoringExecutionHelper
import org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedNamesAssistProposal.DeleteBlockingExitPolicy
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds
import org.eclipse.jdt.ui.actions.SelectionDispatchAction
import org.eclipse.jdt.ui.refactoring.RefactoringSaveHelper
import org.eclipse.jdt.ui.refactoring.RenameSupport
import org.eclipse.jface.text.ITextSelection
import org.eclipse.jface.text.ITextViewerExtension6
import org.eclipse.jface.text.IUndoManager
import org.eclipse.jface.text.IUndoManagerExtension
import org.eclipse.jface.text.link.ILinkedModeListener
import org.eclipse.jface.text.link.LinkedModeModel
import org.eclipse.jface.text.link.LinkedPosition
import org.eclipse.jface.text.link.LinkedPositionGroup
import org.eclipse.ltk.core.refactoring.RefactoringCore
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.model.sourceElementsToLightElements
import org.jetbrains.kotlin.core.references.resolveToSourceDeclaration
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaElementUtil
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.jetbrains.kotlin.eclipse.ui.utils.getTextDocumentOffset
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.ui.editors.KotlinCommonEditor
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit as ContentProviderCompilationUnit

public class KotlinRenameAction(val editor: KotlinCommonEditor) : SelectionDispatchAction(editor.getSite()) {
    init {
        setActionDefinitionId(IJavaEditorActionDefinitionIds.RENAME_ELEMENT)
    }
    
    companion object {
        val ACTION_ID = "RenameElement"
    }
    
    override fun run(selection: ITextSelection) {
        val jetElement = EditorUtil.getJetElement(editor, selection.getOffset())
        if (jetElement == null) return
        
        performRefactoring(jetElement)
    }
    
    fun performRefactoring(jetElement: KtElement) {
        val sourceElements = jetElement.resolveToSourceDeclaration()
        if (sourceElements.isEmpty()) return
        
        beginRenameRefactoring(sourceElements, jetElement, editor)
    }
    
    fun undo(editor: KotlinCommonEditor, startingUndoOperation: IUndoableOperation?) {
        editor.getSite().getWorkbenchWindow().run(false, true) {
            val undoManager = getUndoManager(editor)
            if (undoManager is IUndoManagerExtension) {
                val undoContext = undoManager.getUndoContext()
                val operationHistory = OperationHistoryFactory.getOperationHistory()
                while (undoManager.undoable()) {
                    if (startingUndoOperation != null && startingUndoOperation == operationHistory.getUndoOperation(undoContext)) {
                        return@run
                    }
                    undoManager.undo()
                }
                
            }
        }
    }
    
    fun getCurrentUndoOperation(editor: KotlinCommonEditor): IUndoableOperation? {
        val undoManager = getUndoManager(editor)
        if (undoManager is IUndoManagerExtension) {
            val undoContext = undoManager.getUndoContext()
            val operationHistory = OperationHistoryFactory.getOperationHistory()
            return operationHistory.getUndoOperation(undoContext)
        }
        
        return null
    }
    
    fun getUndoManager(editor: KotlinCommonEditor): IUndoManager? {
        val viewer = editor.getViewer()
        return if (viewer is ITextViewerExtension6) viewer.getUndoManager() else null
    }
    
    fun beginRenameRefactoring(sourceElements: List<SourceElement>, selectedElement: KtElement, editor: KotlinCommonEditor) {
        val linkedPositionGroup = LinkedPositionGroup()
        val offsetInDocument = selectedElement.getTextDocumentOffset(editor.document)
        
        val textLength = getLengthOfIdentifier(selectedElement)
        if (textLength == null) return
        
        val position = LinkedPosition(editor.document, offsetInDocument, textLength)
        linkedPositionGroup.addPosition(position)
        
        val startindUndoOperation = getCurrentUndoOperation(editor)
        
        val linkedModeModel = LinkedModeModel()
        linkedModeModel.addGroup(linkedPositionGroup)
        linkedModeModel.forceInstall()
        linkedModeModel.addLinkingListener(EditorHighlightingSynchronizer(editor))
        linkedModeModel.addLinkingListener(object : ILinkedModeListener {
            override fun left(model: LinkedModeModel, flags: Int) {
                if ((flags and ILinkedModeListener.UPDATE_CARET) != 0) {
                    val newName = position.getContent()
                    undo(editor, startindUndoOperation)
                    
                    KotlinPsiManager.getKotlinFileIfExist(editor.eclipseFile!!, editor.document.get()) // commit document
                    
                    doRename(sourceElements, newName, editor)
                }
            }
            
            override fun resume(model: LinkedModeModel?, flags: Int) {
            }
            
            override fun suspend(model: LinkedModeModel?) {
            }
        })
        
        val ui = EditorLinkedModeUI(linkedModeModel, editor.getViewer())
        ui.setExitPosition(editor.getViewer(), offsetInDocument, 0, Integer.MAX_VALUE)
        ui.setExitPolicy(DeleteBlockingExitPolicy(editor.document))
        ui.enter()
    }
}

private inline fun <T : IJavaElement> wrapIntoLightElementForKotlin(element: T, wrap: (T) -> T): T {
    return if (EclipseJavaElementUtil.isKotlinLightClass(element)) wrap(element) else element
}

fun createRenameSupport(javaElement: IJavaElement, newName: String): RenameSupport {
    val updateStrategy = RenameSupport.UPDATE_REFERENCES
    return when (javaElement) {
        is IType -> {
            val element = wrapIntoLightElementForKotlin(javaElement, ::KotlinLightType)
            RenameSupport.create(element, newName, updateStrategy)
        }
        is IMethod -> {
            if (javaElement.isConstructor) {
                createRenameSupport(javaElement.getDeclaringType(), newName)
            } else {
                val element = wrapIntoLightElementForKotlin(javaElement, ::KotlinLightFunction)
                RenameSupport.create(element, newName, updateStrategy)
            }
        }
        else -> throw UnsupportedOperationException("Rename refactoring for ${javaElement} is not supported")
    }
}

fun doRename(sourceElements: List<SourceElement>, newName: String, editor: KotlinCommonEditor) {
    fun renameByJavaElement(javaElements: List<IJavaElement>) {
        val javaElement = javaElements[0]
        val renameSupport = createRenameSupport(javaElement, newName)
        with(editor.getSite()) {
            renameSupport.perform(getShell(), getWorkbenchWindow())
        }
    }
    
    fun renameLocalKotlinElement(sourceElement: KotlinSourceElement) {
        val helper = RefactoringExecutionHelper(
                RenameRefactoring(KotlinRenameProcessor(sourceElement, newName, editor.isScript)), 
                RefactoringCore.getConditionCheckingFailedSeverity(),
                RefactoringSaveHelper.SAVE_REFACTORING,
                editor.getSite().getShell(),
                editor.getSite().getWorkbenchWindow())
        try {
            helper.perform(true, true)
        } catch (e: InterruptedException) {
            // skip
        }
    }
    
    val lightElements = sourceElementsToLightElements(sourceElements)
    if (lightElements.isNotEmpty()) {
        renameByJavaElement(lightElements)
        editor.eclipseFile!!.getProject().refreshLocal(IResource.DEPTH_INFINITE, NullProgressMonitor())
    } else {
        if (sourceElements.isNotEmpty()) {
            val element = sourceElements.first()
            if (element is KotlinSourceElement) {
                renameLocalKotlinElement(element)
            }
        }
    }
}