package org.jetbrains.kotlin.core.model

import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.lang.ASTNode
import com.intellij.openapi.project.Project
import com.intellij.psi.codeStyle.Indent
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.PsiFile
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiElement
import com.intellij.openapi.util.TextRange
import com.intellij.util.ThrowableRunnable
import com.intellij.openapi.util.Computable
import com.intellij.psi.codeStyle.ChangedRangesInfo

public class DummyCodeStyleManager : CodeStyleManager() {
    override fun reformatTextWithContext(arg0: PsiFile, arg1: ChangedRangesInfo) {
        throw UnsupportedOperationException()
    }

    override fun reformatNewlyAddedElement(block: ASTNode, addedElement: ASTNode) {
        throw UnsupportedOperationException()
    }
    
    override fun getProject(): Project {
        throw UnsupportedOperationException()
    }
    
    override fun fillIndent(indent: Indent?, fileType: FileType?): String? {
        throw UnsupportedOperationException()
    }
    
    override fun getLineIndent(file: PsiFile, offset: Int): String? {
        throw UnsupportedOperationException()
    }
    
    override fun getLineIndent(document: Document, offset: Int): String? {
        throw UnsupportedOperationException()
    }
    
    override fun getIndent(text: String?, fileType: FileType?): Indent? {
        throw UnsupportedOperationException()
    }
    
    override fun zeroIndent(): Indent? {
        throw UnsupportedOperationException()
    }
    
    override fun reformat(element: PsiElement): PsiElement {
        throw UnsupportedOperationException()
    }
    
    override fun reformat(element: PsiElement, canChangeWhiteSpacesOnly: Boolean): PsiElement {
        throw UnsupportedOperationException()
    }
    
    override fun reformatTextWithContext(arg0: PsiFile, arg1: MutableCollection<TextRange>) {
        throw UnsupportedOperationException()
    }
    
    override fun isLineToBeIndented(file: PsiFile, offset: Int): Boolean {
        throw UnsupportedOperationException()
    }
    
    override fun adjustLineIndent(file: PsiFile, rangeToAdjust: TextRange?) {
        // TODO: implement 
    }
    
    override fun adjustLineIndent(file: PsiFile, offset: Int): Int {
        throw UnsupportedOperationException()
    }
    
    override fun adjustLineIndent(document: Document, offset: Int): Int {
        throw UnsupportedOperationException()
    }
    
    override fun isSequentialProcessingAllowed(): Boolean {
        throw UnsupportedOperationException()
    }
    
    override fun reformatText(file: PsiFile, ranges: MutableCollection<TextRange>) {
        throw UnsupportedOperationException()
    }
    
    override fun reformatText(file: PsiFile, startOffset: Int, endOffset: Int) {
        throw UnsupportedOperationException()
    }
    
    override fun reformatRange(element: PsiElement, startOffset: Int, endOffset: Int): PsiElement? = element
    
    override fun reformatRange(element: PsiElement, startOffset: Int, endOffset: Int, canChangeWhiteSpacesOnly: Boolean): PsiElement? = element
    
    override fun performActionWithFormatterDisabled(r: Runnable?) {
        throw UnsupportedOperationException()
    }
    
    override fun <T : Throwable?> performActionWithFormatterDisabled(r: ThrowableRunnable<T>?) {
        throw UnsupportedOperationException()
    }
    
    override fun <T : Any?> performActionWithFormatterDisabled(r: Computable<T>?): T {
        throw UnsupportedOperationException()
    }
}