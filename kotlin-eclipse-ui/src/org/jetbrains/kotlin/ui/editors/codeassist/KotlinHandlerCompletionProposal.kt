package org.jetbrains.kotlin.ui.editors.codeassist

import org.eclipse.jface.text.contentassist.ICompletionProposal
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.DocumentEvent
import org.eclipse.jface.text.ITextViewer

public abstract class KotlinHandlerCompletionProposal(
		val proposal: KotlinCompletionProposal): ICompletionProposal by proposal, ICompletionProposalExtension, ICompletionProposalExtension2 {
	
	override fun selected(viewer: ITextViewer, smartToggle: Boolean) {
	}
	
	override fun unselected(viewer: ITextViewer) {
	}
	
	override fun validate(document: IDocument, offset: Int, event: DocumentEvent): Boolean {
		val replacementLength = offset - proposal.replacementOffset
		return proposal.replacementString.startsWith(document.get(proposal.replacementOffset, replacementLength))
	}
	
	override fun isValidFor(document: IDocument, offset: Int): Boolean {
		throw IllegalStateException("This method should never be called")
	}
	
	override fun apply(document: IDocument, trigger: Char, offset: Int) {
		throw IllegalStateException("This method should never be called")
	}
	
	override fun getContextInformationPosition(): Int = -1
}