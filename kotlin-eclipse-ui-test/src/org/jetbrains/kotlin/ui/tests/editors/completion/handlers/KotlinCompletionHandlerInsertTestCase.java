package org.jetbrains.kotlin.ui.tests.editors.completion.handlers;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.jetbrains.kotlin.testframework.editor.KotlinEditorWithAfterFileTestCase;
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils;
import org.jetbrains.kotlin.testframework.utils.ExpectedCompletionUtils;
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils;
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor;
import org.jetbrains.kotlin.ui.editors.codeassist.KotlinCompletionProcessor;

public abstract class KotlinCompletionHandlerInsertTestCase extends KotlinEditorWithAfterFileTestCase {

	@Override
	protected void performTest(String fileText, String expectedFileText) {
		ICompletionProposal[] proposals = getActualProposals(getEditor());
		
		String itemToComplete = ExpectedCompletionUtils.itemToComplete(fileText);
		assert itemToComplete == null && proposals.length == 1 : "Completion proposal ambiguity";
		if (itemToComplete == null) {
			itemToComplete = "";
		}

		char completionChar = ' ';
		Character completionCharacter = ExpectedCompletionUtils.getCompletionChar(fileText);
		if (completionCharacter != null) {
			completionChar = completionCharacter;
		}
		
		for (ICompletionProposal proposal : proposals) {
			if (proposal.getDisplayString().startsWith(itemToComplete)) {
				if (proposal instanceof ICompletionProposalExtension2) {
					ICompletionProposalExtension2 proposalExtension = (ICompletionProposalExtension2) proposal;
					proposalExtension.apply(getEditor().getViewer(), completionChar, 0, getCaret());
				} else {
					proposal.apply(getEditor().getViewer().getDocument());
				}
				break;
			}
		}
		
		EditorTestUtils.assertByEditor(getEditor(), expectedFileText);
	}
	
	private ICompletionProposal[] getActualProposals(KotlinFileEditor javaEditor) {
		KotlinCompletionProcessor ktCompletionProcessor = new KotlinCompletionProcessor(javaEditor);
		ICompletionProposal[] proposals = ktCompletionProcessor.computeCompletionProposals(
				javaEditor.getViewer(), 
				KotlinTestUtils.getCaret(javaEditor));
		
		return proposals;
	}

}
