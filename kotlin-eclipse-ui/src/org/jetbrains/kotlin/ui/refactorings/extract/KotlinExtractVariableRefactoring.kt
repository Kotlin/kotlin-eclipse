package org.jetbrains.kotlin.ui.refactorings.extract

import org.eclipse.ltk.core.refactoring.Refactoring
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.ltk.core.refactoring.RefactoringStatus
import org.eclipse.ltk.core.refactoring.Change
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages
import org.jetbrains.kotlin.psi.KtExpression
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtContainerNode
import org.jetbrains.kotlin.psi.KtWhenEntry
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtLoopExpression
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.core.model.KotlinAnalysisFileCache
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.eclipse.ltk.core.refactoring.NullChange
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.eclipse.jface.text.IDocument
import org.jetbrains.kotlin.eclipse.ui.utils.getTextDocumentOffset
import org.jetbrains.kotlin.eclipse.ui.utils.getOffsetByDocument
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.jetbrains.kotlin.ui.refactorings.rename.FileEdit
import org.eclipse.text.edits.TextEdit
import org.eclipse.text.edits.ReplaceEdit
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.eclipse.jface.text.TextUtilities
import org.eclipse.ltk.core.refactoring.TextFileChange
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility
import org.jetbrains.kotlin.ui.formatter.AlignmentStrategy
import org.eclipse.jface.text.ITextSelection
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsStatement

public class KotlinExtractVariableRefactoring(val selection: ITextSelection, val editor: KotlinFileEditor) : Refactoring() {
    public var newName: String = "temp"
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
        val fileChange = TextFileChange("Introduce variable", editor.getFile()!!)
        edits.forEach { TextChangeCompatibility.addTextEdit(fileChange, "Kotlin change", it.edit) }
        
        return fileChange
    }
    
    private fun getBindingContext(): BindingContext {
        val javaProject = KotlinPsiManager.getJavaProject(expression)!!
        val analysisResult = KotlinAnalysisFileCache.getAnalysisResult(expression.getContainingJetFile(), javaProject).analysisResult
        return analysisResult.bindingContext
    }
    
    private fun introduceVariable(): List<FileEdit> {
        val allReplaces = listOf(expression)
        val commonParent = PsiTreeUtil.findCommonParent(allReplaces)
        if (commonParent == null) return emptyList()
        
        val commonContainer = getContainer(commonParent)
        if (commonContainer == null) return emptyList()
        
        val anchor = calculateAnchor(commonParent, commonContainer, allReplaces)
        if (anchor == null) return emptyList()
        
        val newLine = KtPsiFactory(expression).createNewLine()
        val indent = AlignmentStrategy.computeIndent(commonContainer.getFirstChild().getNode())
        val lineDelimiter = TextUtilities.getDefaultLineDelimiter(editor.document)
        val newLineWithShift = AlignmentStrategy.alignCode(newLine.getNode(), indent, lineDelimiter)
        
        val variableText = "val $newName = ${expression.getText()}"
        
        val bindingContext = getBindingContext()
        return if (expression.isUsedAsStatement(bindingContext)) {
            listOf(replaceExpressionWithVariableDeclaration(variableText))
        } else {
            listOf(insertBefore(anchor, "$variableText${newLineWithShift}")) + listOf(replaceOccurrence())
        }
    }
    
    private fun shouldReplaceInitialExpression(context: BindingContext): Boolean {
        return expression.isUsedAsStatement(context)
    }
    
    private fun replaceExpressionWithVariableDeclaration(variableText: String): FileEdit {
        val offset = expression.getTextDocumentOffset(editor.document)
        return FileEdit(editor.getFile()!!, ReplaceEdit(offset, expression.getTextLength(), variableText))
    }
    
    private fun replaceOccurrence(): FileEdit {
        val offset = expression.getTextDocumentOffset(editor.document)
        return FileEdit(editor.getFile()!!, ReplaceEdit(offset, expression.getTextLength(), newName))
    }
    
    private fun insertBefore(psiElement: PsiElement, text: String): FileEdit {
        val startOffset = psiElement.getOffsetByDocument(editor.document, psiElement.getTextRange().getStartOffset())
        return FileEdit(editor.getFile()!!, ReplaceEdit(startOffset, 0, text))
    }
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