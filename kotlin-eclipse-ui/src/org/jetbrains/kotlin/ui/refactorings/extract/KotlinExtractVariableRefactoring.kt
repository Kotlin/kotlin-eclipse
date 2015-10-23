package org.jetbrains.kotlin.ui.refactorings.extract

import org.eclipse.ltk.core.refactoring.Refactoring
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.ltk.core.refactoring.RefactoringStatus
import org.eclipse.ltk.core.refactoring.Change
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages
import org.jetbrains.kotlin.psi.JetExpression
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.JetBlockExpression
import org.jetbrains.kotlin.psi.JetContainerNode
import org.jetbrains.kotlin.psi.JetWhenEntry
import org.jetbrains.kotlin.psi.JetDeclarationWithBody
import org.jetbrains.kotlin.psi.JetIfExpression
import org.jetbrains.kotlin.psi.JetLoopExpression
import org.jetbrains.kotlin.psi.JetArrayAccessExpression
import org.jetbrains.kotlin.core.model.KotlinAnalysisFileCache
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.eclipse.ltk.core.refactoring.NullChange
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.eclipse.jface.text.IDocument
import org.jetbrains.kotlin.eclipse.ui.utils.getTextDocumentOffset
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.jetbrains.kotlin.ui.refactorings.rename.FileEdit
import org.eclipse.text.edits.TextEdit
import org.eclipse.text.edits.ReplaceEdit
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.eclipse.jface.text.TextUtilities
import org.eclipse.ltk.core.refactoring.TextFileChange
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility
import org.jetbrains.kotlin.ui.formatter.AlignmentStrategy

public class KotlinExtractVariableRefactoring(val expression: JetExpression, val editor: KotlinFileEditor) : Refactoring() {
    public var newName: String = "temp"
    private val psiFactory = JetPsiFactory(expression)
    
    override fun checkFinalConditions(pm: IProgressMonitor?): RefactoringStatus = RefactoringStatus()
    
    override fun checkInitialConditions(pm: IProgressMonitor?): RefactoringStatus? = RefactoringStatus()
    
    override fun getName(): String = RefactoringCoreMessages.ExtractTempRefactoring_name
    
    override fun createChange(pm: IProgressMonitor?): Change {
        val edits = introduceVariable()
        val fileChange = TextFileChange("Introduce variable", editor.getFile()!!)
        edits.forEach { TextChangeCompatibility.addTextEdit(fileChange, "Kotlin change", it.edit) }
        
        return fileChange
    }
    
//    private fun doRefactoring() {
//        val javaProject = KotlinPsiManager.getJavaProject(expression)
//        if (javaProject == null) return
//        
//        val analysisResult = KotlinAnalysisFileCache.getAnalysisResult(expression.getContainingJetFile(), javaProject).analysisResult
//        val bindingContext = analysisResult.bindingContext
//        val expressionType = bindingContext.getType(expression)
//        
//        
//    }
    
    private fun introduceVariable(): List<FileEdit> {
        val commonParent = PsiTreeUtil.findCommonParent(listOf(expression))
        if (commonParent == null) return emptyList()
        
        val commonContainer = getContainer(commonParent)
        if (commonContainer == null) return emptyList()
        
        val anchor = calculateAnchor(commonParent, commonContainer, listOf(expression))
        if (anchor == null) return emptyList()
        
        val newLine = psiFactory.createNewLine()
        val indent = AlignmentStrategy.computeIndent(expression.getNode())
        val lineDelimiter = TextUtilities.getDefaultLineDelimiter(editor.document)
        val newLineWithShift = AlignmentStrategy.alignCode(newLine.getNode(), indent, lineDelimiter)
        
        val variableText = "val $newName = ${expression.getText()}${newLineWithShift}"
        
        return listOf(insertBefore(anchor, variableText))
    }
    
    private fun insertBefore(psiElement: PsiElement, text: String): FileEdit {
        val startOffset = psiElement.getTextDocumentOffset(editor.document)
        return FileEdit(editor.getFile()!!, ReplaceEdit(startOffset, 0, text))
    }
}


private fun calculateAnchor(commonParent: PsiElement, commonContainer: PsiElement, allReplaces: List<JetExpression>): PsiElement? {
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
    if (place is JetBlockExpression) return place
    
    var container = place
    while (container != null) {
        val parent = container.getParent()
        if (parent is JetContainerNode) {
            if (!isBadContainerNode(parent, container)) {
                return parent
            }
        }
        
        if (parent is JetBlockExpression || (parent is JetWhenEntry && container == parent.getExpression())) {
            return parent
        }
        
        if (parent is JetDeclarationWithBody && parent.getBodyExpression() == container) {
            return parent
        }
        
        container = parent
    }
    
    return null
}

private fun isBadContainerNode(parent: JetContainerNode, place: PsiElement): Boolean {
    if (parent.getParent() is JetIfExpression && (parent.getParent() as JetIfExpression).getCondition() == place) {
        return true
    } else if (parent.getParent() is JetLoopExpression && (parent.getParent() as JetLoopExpression).getBody() != place) {
        return true
    } else if (parent.getParent() is JetArrayAccessExpression) {
        return true
    }
    
    return false
}