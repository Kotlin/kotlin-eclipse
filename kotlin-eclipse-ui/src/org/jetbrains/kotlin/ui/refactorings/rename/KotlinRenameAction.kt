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

import org.eclipse.jdt.ui.actions.SelectionDispatchAction
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.eclipse.jface.text.ITextSelection
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.jetbrains.kotlin.core.references.getReferenceExpression
import org.jetbrains.kotlin.core.references.createReference
import org.jetbrains.kotlin.core.references.resolveToSourceElements
import org.jetbrains.kotlin.core.model.sourceElementsToLightElements
import org.eclipse.jdt.internal.ui.refactoring.reorg.RenameLinkedMode
import org.eclipse.jdt.core.IJavaElement
import org.eclipse.jface.text.link.LinkedModeModel
import org.eclipse.jface.text.link.LinkedPositionGroup
import org.eclipse.jface.text.link.LinkedPosition
import com.intellij.psi.PsiElement
import org.eclipse.jdt.internal.ui.javaeditor.EditorHighlightingSynchronizer
import org.eclipse.jface.text.link.ILinkedModeListener
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI
import org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedNamesAssistProposal.DeleteBlockingExitPolicy
import org.jetbrains.kotlin.eclipse.ui.utils.getTextDocumentOffset
import org.eclipse.jdt.core.refactoring.IJavaRefactorings
import org.eclipse.ltk.core.refactoring.RefactoringCore
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor
import org.eclipse.jdt.ui.refactoring.RenameSupport
import org.eclipse.jdt.core.IType
import org.eclipse.core.resources.IResource
import org.eclipse.core.resources.IFile
import org.eclipse.jdt.core.ICompilationUnit
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility
import org.eclipse.jdt.core.ISourceRange
import org.eclipse.jdt.core.IMethod
import org.eclipse.jdt.internal.core.CompilationUnit
import org.eclipse.text.edits.TextEdit
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.text.edits.UndoEdit
import org.eclipse.jdt.core.compiler.CharOperation
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit as ContentProviderCompilationUnit

public class KotlinRenameAction(val editor: KotlinFileEditor) : SelectionDispatchAction(editor.getSite()) {
    init {
        setActionDefinitionId(IJavaEditorActionDefinitionIds.RENAME_ELEMENT)
    }
    
    companion object {
        val ACTION_ID = "RenameElement"
    }
    
    override fun run(selection: ITextSelection) {
        val psiElement = EditorUtil.getPsiElement(editor, selection.getOffset())
        if (psiElement == null) return
        
        val expression = getReferenceExpression(psiElement)
        if (expression == null) return
        
        val sourceElements = createReference(expression).resolveToSourceElements()
        val javaElements = sourceElementsToLightElements(sourceElements, editor.javaProject!!)
        if (javaElements.isEmpty()) return
        
        beginRenameRefactoring(javaElements[0], psiElement, editor)
    }
}

fun beginRenameRefactoring(javaElement: IJavaElement, psiElement: PsiElement, editor: KotlinFileEditor) {
    val linkedPositionGroup = LinkedPositionGroup()
    val offsetInDocument = psiElement.getTextDocumentOffset(editor.document)
    linkedPositionGroup.addPosition(LinkedPosition(editor.document, offsetInDocument, psiElement.getTextLength()))
    
    val linkedModeModel = LinkedModeModel()
    linkedModeModel.addGroup(linkedPositionGroup)
    linkedModeModel.forceInstall()
    linkedModeModel.addLinkingListener(EditorHighlightingSynchronizer(editor))
    linkedModeModel.addLinkingListener(object : ILinkedModeListener {
        override fun left(model: LinkedModeModel, flags: Int) {
            if ((flags and ILinkedModeListener.UPDATE_CARET) != 0) {
                doRename(javaElement, editor)
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

fun doRename(javaElement: IJavaElement, editor: KotlinFileEditor) {
    val contributionId = IJavaRefactorings.RENAME_TYPE
    val descriptor = RefactoringCore.getRefactoringContribution(contributionId).createDescriptor() as RenameJavaElementDescriptor
    descriptor.setJavaElement(javaElement)
    descriptor.setNewName("${javaElement.getElementName()}K")
    descriptor.setUpdateReferences(true)
    
    val kotlinLightType = KotlinLightType(javaElement as IType, editor)
    val renameSupport = RenameSupport.create(kotlinLightType, "${javaElement.getElementName()}K", RenameSupport.UPDATE_REFERENCES)
    with(editor.getSite()) {
        renameSupport.perform(getShell(), getWorkbenchWindow())
    }
}

class KotlinLightType(javaElement: IType, val editor: KotlinFileEditor) : IType by javaElement {
    override fun getCompilationUnit(): ICompilationUnit {
        val compilationUnit = EditorUtility.getEditorInputJavaElement(editor, false) as CompilationUnit
        return KotlinLightCompilationUnit(editor.getFile()!!, compilationUnit)
    }
    
    override fun getMethods(): Array<out IMethod> {
        return emptyArray()
    }

    override fun getNameRange(): ISourceRange {
        return object : ISourceRange {
            override fun getLength(): Int {
                return 0
            }
            
            override fun getOffset(): Int {
                return 0
            }
        }
    }

    override fun getPrimaryElement(): IJavaElement? = this

    override fun isBinary(): Boolean = false

    override fun isReadOnly(): Boolean = false
}

class KotlinLightCompilationUnit(val file: IFile, compilationUnit: ICompilationUnit) : ICompilationUnit by compilationUnit,
       ContentProviderCompilationUnit {
    override fun getFileName(): CharArray? = CharOperation.NO_CHAR
    
    override fun getContents(): CharArray? = CharOperation.NO_CHAR
    
    override fun ignoreOptionalProblems(): Boolean = true
    
    override fun getMainTypeName(): CharArray? = CharOperation.NO_CHAR
    
    override fun getPackageName(): Array<out CharArray>? {
        return null
    }

    override fun getResource(): IResource = file
}