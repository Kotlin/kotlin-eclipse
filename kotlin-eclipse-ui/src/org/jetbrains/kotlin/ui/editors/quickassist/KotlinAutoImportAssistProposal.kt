package org.jetbrains.kotlin.ui.editors.quickassist

import org.eclipse.core.resources.IFile
import org.eclipse.jdt.core.IType
import org.eclipse.jdt.ui.ISharedImages
import org.eclipse.jdt.ui.JavaUI
import org.eclipse.jface.text.BadLocationException
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.TextUtilities
import org.eclipse.swt.graphics.Image
import org.eclipse.ui.texteditor.AbstractTextEditor
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.jetbrains.kotlin.eclipse.ui.utils.IndenterUtil
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetImportDirective
import org.jetbrains.kotlin.psi.JetImportList
import org.jetbrains.kotlin.psi.JetPackageDirective
import org.jetbrains.kotlin.ui.editors.KotlinEditor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.eclipse.ui.utils.getEndLfOffset

public class KotlinAutoImportAssistProposal(val proposalType: IType) : KotlinQuickAssistProposal() {
	override public fun apply(document: IDocument, psiElement: PsiElement) {
		val editor = getActiveEditor()
		if (editor == null) {
			return
		}
		
		val file = EditorUtil.getFile(editor)
		if (file == null) {
			KotlinLogger.logError("Failed to retrieve IFile from editor " + editor, null)
			return
		}
		
		val placeElement = findNodeToNewImport(file)
        if (placeElement == null) return
        
		val breakLineBefore = computeBreakLineBeforeImport(placeElement)
		val breakLineAfter = computeBreakLineAfterImport(placeElement)
		val lineDelimiter = TextUtilities.getDefaultLineDelimiter(document)
		val newImport = "${IndenterUtil.createWhiteSpace(0, breakLineBefore, lineDelimiter)}import ${getFqName()}" +
				"${IndenterUtil.createWhiteSpace(0, breakLineAfter, lineDelimiter)}"
		document.replace(getOffset(placeElement, editor), 0, newImport)
	}
	
	override public fun isApplicable(psiElement: PsiElement): Boolean = true
	
	override public fun getImage(): Image = JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_IMPDECL)
	
	override public fun getDisplayString(): String {
		return "Import '${proposalType.getElementName()}' (${proposalType.getPackageFragment().getElementName()})"
	}
	
	public fun getFqName(): String = proposalType.getFullyQualifiedName('.')
	
	private fun getOffset(element: PsiElement, editor: AbstractTextEditor): Int {
		return element.getEndLfOffset(EditorUtil.getDocument(editor))
	}
	
	private fun computeBreakLineAfterImport(element: PsiElement): Int {
		if (element is JetPackageDirective) {
			val nextSibling = element.getNextSibling()
			if (nextSibling is JetImportList) {
				val importList = nextSibling
				if (importList.getImports().isNotEmpty()) {
					return 2
				} else {
					return countBreakLineAfterImportList(nextSibling.getNextSibling())
				}
			}
		}
		
		return 0
	}
	
	private fun countBreakLineAfterImportList(psiElement: PsiElement):Int {
		if (psiElement is PsiWhiteSpace) {
			val countBreakLineAfterHeader = IndenterUtil.getLineSeparatorsOccurences(psiElement.getText())
			return when (countBreakLineAfterHeader) {
				0 -> 2
				1 -> 1
				else -> 0
			}
		}
		
		return 2
	}
	
	private fun computeBreakLineBeforeImport(element:PsiElement):Int {
		if (element is JetPackageDirective) {
			return when {
				element.isRoot() -> 0
				else -> 2
			} 
		}
		
		return 1
	}
    
	private fun findNodeToNewImport(file:IFile): PsiElement? {
		val jetFile = KotlinPsiManager.INSTANCE.getParsedFile(file)
		val jetImportDirective = jetFile.getImportDirectives()
		return if (jetImportDirective.isNotEmpty()) jetImportDirective.last() else jetFile.getPackageDirective()
	}
}