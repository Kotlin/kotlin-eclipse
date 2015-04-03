package org.jetbrains.kotlin.ui.tests.editors.completion.handlers;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.jetbrains.kotlin.testframework.editor.KotlinEditorWithAfterFileTestCase;
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils;
import org.jetbrains.kotlin.testframework.utils.ExpectedCompletionUtils;
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils;
import org.jetbrains.kotlin.ui.editors.KotlinEditor;
import org.jetbrains.kotlin.ui.editors.codeassist.KotlinCompletionProcessor;

public abstract class KotlinFunctionCompletionTestCase extends KotlinEditorWithAfterFileTestCase {

	@Override
	protected void performTest(String fileText, String expectedFileText) {
		ICompletionProposal[] proposals = getActualProposals(getEditor());
		
		String itemToComplete = "";
		if (proposals.length > 1) {
			itemToComplete = ExpectedCompletionUtils.itemToComplete(fileText);
		}
		
		for (ICompletionProposal proposal : proposals) {
			if (proposal.getDisplayString().startsWith(itemToComplete)) {
				if (!(proposal instanceof ICompletionProposalExtension2)) {
					throw new IllegalStateException("Completion with handler proposal should implement ICompletionProposalExtension2");
				}
				ICompletionProposalExtension2 proposalExtension = (ICompletionProposalExtension2) proposal;
				proposalExtension.apply(getEditor().getViewer(), ' ', 0, getCaret());
			}
		}
		
		EditorTestUtils.assertByEditor(getEditor(), expectedFileText);
	}
	
	private ICompletionProposal[] getActualProposals(KotlinEditor javaEditor) {
		KotlinCompletionProcessor ktCompletionProcessor = new KotlinCompletionProcessor(javaEditor);
		ICompletionProposal[] proposals = ktCompletionProcessor.computeCompletionProposals(
				javaEditor.getViewer(), 
				KotlinTestUtils.getCaret(javaEditor));
		
		return proposals;
	}

}
