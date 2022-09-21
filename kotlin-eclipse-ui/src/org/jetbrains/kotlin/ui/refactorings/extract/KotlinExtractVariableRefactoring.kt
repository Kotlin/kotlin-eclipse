/*******************************************************************************
* Copyright 2000-2016 JetBrains s.r.o.
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
package org.jetbrains.kotlin.ui.refactorings.extract

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility
import org.eclipse.jface.text.ITextSelection
import org.eclipse.jface.text.TextUtilities
import org.eclipse.ltk.core.refactoring.Change
import org.eclipse.ltk.core.refactoring.Refactoring
import org.eclipse.ltk.core.refactoring.RefactoringStatus
import org.eclipse.ltk.core.refactoring.TextFileChange
import org.eclipse.text.edits.ReplaceEdit
import org.jetbrains.kotlin.core.utils.getBindingContext
import org.jetbrains.kotlin.eclipse.ui.utils.IndenterUtil
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil
import org.jetbrains.kotlin.eclipse.ui.utils.getOffsetByDocument
import org.jetbrains.kotlin.eclipse.ui.utils.getTextDocumentOffset
import org.jetbrains.kotlin.idea.util.psi.patternMatching.KotlinPsiUnifier
import org.jetbrains.kotlin.idea.util.psi.patternMatching.toRange
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.canPlaceAfterSimpleNameEntry
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsStatement
import org.jetbrains.kotlin.ui.editors.KotlinCommonEditor
import org.jetbrains.kotlin.ui.formatter.AlignmentStrategy
import org.jetbrains.kotlin.ui.refactorings.rename.FileEdit

public class KotlinExtractVariableRefactoring(val selection: ITextSelection, val editor: KotlinCommonEditor) : Refactoring() {
    public var newName: String = "temp"
    public var replaceAllOccurrences = true
    private lateinit var expression: KtExpression
    override fun checkFinalConditions(pm: IProgressMonitor?): RefactoringStatus = RefactoringStatus()
    
    override fun checkInitialConditions(pm: IProgressMonitor?): RefactoringStatus {
        val startOffset = LineEndUtil.convertCrToDocumentOffset(editor.document, selection.getOffset())
        val selectedExpression = PsiTreeUtil.findElementOfClassAtRange(editor.parsedFile!!, startOffset, 
            startOffset + selection.getLength(), KtExpression::class.java)
        return if (selectedExpression != null) {
            expression = selectedExpression
            RefactoringStatus()
        } else {
            RefactoringStatus.createErrorStatus("Could not extract variable")
        }
    }
    
    override fun getName(): String = RefactoringCoreMessages.ExtractTempRefactoring_name
    
    override fun createChange(pm: IProgressMonitor?): Change {
        val edits = introduceVariable()
        val fileChange = TextFileChange("Introduce variable", editor.eclipseFile!!)
        edits.forEach { TextChangeCompatibility.addTextEdit(fileChange, "Kotlin change", it.edit) }
        
        return fileChange
    }
    
    private fun introduceVariable(): List<FileEdit> {
        val occurrenceContainer = expression.getOccurrenceContainer()
        if (occurrenceContainer == null) return emptyList()
        val allReplaces = if (replaceAllOccurrences) expression.findOccurrences(occurrenceContainer) else listOf(expression)
        val commonParent = PsiTreeUtil.findCommonParent(allReplaces) as KtElement
        val commonContainer = getContainer(commonParent)
        if (commonContainer == null) return emptyList()
        
        val anchor = calculateAnchor(commonParent, commonContainer, allReplaces)
        if (anchor == null) return emptyList()
        
        val indent = run {
            val indentNode = commonContainer.getFirstChild().getNode()
            val nodeIndent = AlignmentStrategy.computeIndent(indentNode)
            if (AlignmentStrategy.isBlockElement(indentNode)) nodeIndent - 1 else nodeIndent
        }
        val variableDeclarationText = "val $newName = ${expression.getText()}"
        
        val bindingContext = expression.getBindingContext()
        
        return createEdits(
                commonContainer,
                expression.isUsedAsStatement(bindingContext),
                commonContainer !is KtBlockExpression,
                variableDeclarationText,
                indent,
                anchor,
                allReplaces)
    }
    
    private fun shouldReplaceInitialExpression(context: BindingContext): Boolean {
        return expression.isUsedAsStatement(context)
    }
    
    private fun replaceExpressionWithVariableDeclaration(variableText: String, firstOccurrence: KtExpression): FileEdit {
        val offset = firstOccurrence.getTextDocumentOffset(editor.document)
        return FileEdit(editor.eclipseFile!!, ReplaceEdit(offset, firstOccurrence.getTextLength(), variableText))
    }
    
    private fun createEdits(
            container: PsiElement,
            isUsedAsStatement: Boolean, 
            needBraces: Boolean, 
            variableDeclarationText: String,
            indent: Int,
            anchor: PsiElement,
            replaces: List<KtExpression>): List<FileEdit> {
        val lineDelimiter = TextUtilities.getDefaultLineDelimiter(editor.document)
        val newLineWithShift = IndenterUtil.createWhiteSpace(indent, 1, lineDelimiter)
        
        val newLineBeforeBrace = IndenterUtil.createWhiteSpace(indent - 1, 1, lineDelimiter)
        
        val sortedReplaces = replaces.sortedBy { it.getTextOffset() }
        if (isUsedAsStatement && sortedReplaces.first() == anchor) {
            val variableText = if (needBraces) {
                "{${newLineWithShift}${variableDeclarationText}${newLineBeforeBrace}}"
            } else {
                variableDeclarationText
            }
            
            val replacesList = sortedReplaces.drop(1).map { replaceOccurrence(newName, it, editor) }
            
            return listOf(replaceExpressionWithVariableDeclaration(variableText, sortedReplaces.first())) + replacesList
        } else {
            val replacesList = replaces.map { replaceOccurrence(newName, it, editor) }
            if (needBraces) {
                val variableText = "{${newLineWithShift}${variableDeclarationText}${newLineWithShift}"
                val removeNewLineIfNeeded = if (isElseAfterContainer(container)) 
                    removeNewLineAfter(container, editor)
                else 
                    null
                
                return listOf(
                        insertBefore(anchor, variableText, editor), 
                        addBraceAfter(expression, newLineBeforeBrace, editor),
                        removeNewLineIfNeeded).filterNotNull() + replacesList
            } else {
                val variableText = "${variableDeclarationText}${newLineWithShift}"
                return replacesList + insertBefore(anchor, variableText, editor)
            }
        }
    }
}

private fun KtExpression.findOccurrences(occurrenceContainer: PsiElement): List<KtExpression> {
    return toRange()
            .match(occurrenceContainer, KotlinPsiUnifier.DEFAULT)
            .map {
                val candidate = it.range.elements.first()
                when (candidate) {
                    is KtExpression -> candidate
                    is KtStringTemplateEntryWithExpression -> candidate.expression
                    else -> throw AssertionError("Unexpected candidate element: " + candidate.text)
                } as? KtExpression
            }
            .filterNotNull()
}

private fun addBraceAfter(expr: KtExpression, newLineBeforeBrace: String, editor: KotlinCommonEditor): FileEdit {
    val parent = expr.getParent()
    var endOffset = parent.getTextRange().getEndOffset()
    val text = editor.document.get()
    while (endOffset > 0 && text[endOffset] == ' ') {
        endOffset--
    }
    
    val offset = expr.getParent().let { it.getOffsetByDocument(editor.document, endOffset + 1) }
    return FileEdit(editor.eclipseFile!!, ReplaceEdit(offset, 0, "$newLineBeforeBrace}"))
}

private fun removeNewLineAfter(container: PsiElement, editor: KotlinCommonEditor): FileEdit? {
    val next = container.nextSibling
    if (next is PsiWhiteSpace) {
        return FileEdit(
                editor.eclipseFile!!, 
                ReplaceEdit(next.getTextDocumentOffset(editor.document), next.getTextLength(), " "))
    }
    
    return null
}

private fun replaceOccurrence(newName: String, replaceExpression: KtExpression, editor: KotlinCommonEditor): FileEdit {
    val (offset, length) = replaceExpression.getReplacementRange(editor)
    return FileEdit(editor.eclipseFile!!, ReplaceEdit(offset, length, newName))
}

private fun KtExpression.getReplacementRange(editor: KotlinCommonEditor): ReplacementRange {
    val p = getParent()
    if (p is KtBlockStringTemplateEntry) {
        if (canPlaceAfterSimpleNameEntry(p.nextSibling)) {
            return ReplacementRange(p.getTextDocumentOffset(editor.document) + 1, p.getTextLength() - 1) // '+- 1' is for '$' sign
        }
    }
    
    return ReplacementRange(getTextDocumentOffset(editor.document), getTextLength())
}

private data class ReplacementRange(val offset: Int, val length: Int)

private fun isElseAfterContainer(container: PsiElement): Boolean {
    val next = container.nextSibling
    if (next != null) {
        val nextnext = next.nextSibling
        if (nextnext != null && nextnext.node.elementType == KtTokens.ELSE_KEYWORD) {
            return true
        }
    }
    
    return false
}

private fun insertBefore(psiElement: PsiElement, text: String, editor: KotlinCommonEditor): FileEdit {
    val startOffset = psiElement.getOffsetByDocument(editor.document, psiElement.getTextRange().getStartOffset())
    return FileEdit(editor.eclipseFile!!, ReplaceEdit(startOffset, 0, text))
}

private fun calculateAnchor(commonParent: PsiElement, commonContainer: PsiElement, allReplaces: List<KtExpression>): PsiElement? {
    var anchor = commonParent
    if (anchor != commonContainer) {
        while (anchor.getParent() != commonContainer) {
            anchor = anchor.getParent()
        }
    } else {
        anchor = commonContainer.getFirstChild()
        var startOffset = commonContainer.getTextRange().getEndOffset()
        for (expr in allReplaces) {
            val offset = expr.getTextRange().getStartOffset()
            if (offset < startOffset) startOffset = offset
        }
        
        while (anchor != null && !anchor.getTextRange().contains(startOffset)) {
            anchor = anchor.getNextSibling()
        }
    }
    
    return anchor
    
}

private fun getContainer(place: PsiElement): PsiElement? {
    if (place is KtBlockExpression) return place
    
    var container = place
    while (container != null) {
        val parent = container.getParent()
        if (parent is KtContainerNode) {
            if (!isBadContainerNode(parent, container)) {
                return parent
            }
        }
        
        if (parent is KtBlockExpression || (parent is KtWhenEntry && container == parent.getExpression())) {
            return parent
        }
        
        if (parent is KtDeclarationWithBody && parent.getBodyExpression() == container) {
            return parent
        }
        
        container = parent
    }
    
    return null
}

private fun KtExpression.getOccurrenceContainer(): KtElement? {
    var result: KtElement? = null
    for ((place, parent) in parentsWithSelf.zip(parents)) {
        when {
            parent is KtContainerNode && place !is KtBlockExpression && !isBadContainerNode(parent, place) -> result = parent
            parent is KtClassBody || parent is KtFile -> return result
            parent is KtBlockExpression -> result = parent
            parent is KtWhenEntry && place !is KtBlockExpression -> result = parent
            parent is KtDeclarationWithBody && parent.bodyExpression == place && place !is KtBlockExpression -> result = parent
        }
    }

    return null
}

public val PsiElement.parentsWithSelf: Sequence<PsiElement>
    get() = generateSequence(this) { if (it is PsiFile) null else it.getParent() }

public val PsiElement.parents: Sequence<PsiElement>
    get() = parentsWithSelf.drop(1)

private fun isBadContainerNode(parent: KtContainerNode, place: PsiElement): Boolean {
    if (parent.getParent() is KtIfExpression && (parent.getParent() as KtIfExpression).getCondition() == place) {
        return true
    } else if (parent.getParent() is KtLoopExpression && (parent.getParent() as KtLoopExpression).getBody() != place) {
        return true
    } else if (parent.getParent() is KtArrayAccessExpression) {
        return true
    }
    
    return false
}