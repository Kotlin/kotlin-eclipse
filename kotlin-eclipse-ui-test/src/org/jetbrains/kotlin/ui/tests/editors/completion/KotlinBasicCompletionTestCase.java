/*******************************************************************************
 * Copyright 2000-2014 JetBrains s.r.o.
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
package org.jetbrains.kotlin.ui.tests.editors.completion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;




import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.jetbrains.kotlin.testframework.editor.KotlinEditorTestCase;
import org.jetbrains.kotlin.testframework.utils.ExpectedCompletionUtils;
import org.jetbrains.kotlin.ui.editors.codeassist.CompletionProcessor;
import org.junit.Assert;

public class KotlinBasicCompletionTestCase extends KotlinEditorTestCase {

	protected void doTest(String testPath) {
		String fileText = getText(testPath);
		testEditor = configureEditor(getNameByPath(testPath), fileText);

		joinBuildThread();

		List<String> actualProposals = getActualProposals();

		Integer expectedNumber = ExpectedCompletionUtils.numberOfItemsShouldPresent(fileText);
		if (expectedNumber != null) {
			Assert.assertEquals("Different count of expected and actual proposals", expectedNumber.intValue(), actualProposals.size());
		}

		Set<String> proposalSet = new HashSet<String>(actualProposals);
		
		List<String> expectedProposals = ExpectedCompletionUtils.itemsShouldExist(fileText);
		assertExists(expectedProposals, proposalSet);
		
		List<String> expectedJavaOnlyProposals = ExpectedCompletionUtils.itemsJavaOnlyShouldExists(fileText);
		assertExists(expectedJavaOnlyProposals, proposalSet);
		
		List<String> unexpectedProposals = ExpectedCompletionUtils.itemsShouldAbsent(fileText);
		assertNotExists(unexpectedProposals, proposalSet);
	}
	
	private List<String> getActualProposals() {
		CompletionProcessor ktCompletionProcessor = new CompletionProcessor(testEditor.getEditor());
		ICompletionProposal[] proposals = ktCompletionProcessor.computeCompletionProposals(testEditor.getEditor().getViewer(), getCaret());
		
		List<String> actualProposals = new ArrayList<String>();
		for (ICompletionProposal proposal : proposals) {
			String replacementString = proposal.getAdditionalProposalInfo();
			if (replacementString == null) replacementString = proposal.getDisplayString();

			actualProposals.add(replacementString);
		}
		
		return actualProposals;
	}

	private void assertExists(List<String> itemsShouldExist, Set<String> actualItems) {
		String errorMessage = getErrorMessage(itemsShouldExist, actualItems);
		for (String itemShouldExist : itemsShouldExist) {
			Assert.assertTrue(errorMessage, actualItems.contains(itemShouldExist.trim()));
		}
	}
	
	private void assertNotExists(List<String> itemsShouldAbsent, Set<String> actualItems) {
		String errorMessage = getErrorMessage(itemsShouldAbsent, actualItems);
		for (String itemShouldAbsent : itemsShouldAbsent) {
			Assert.assertFalse(errorMessage, actualItems.contains(itemShouldAbsent));
		}
	}
	
	private String getErrorMessage(List<String> expected, Set<String> actual) {
		StringBuilder errorMessage = new StringBuilder();
		
		errorMessage.append("Expected: <");
		for (String proposal : expected) {
			errorMessage.append(proposal);
			errorMessage.append(" ");
		}
		errorMessage.append("> ");
		
		errorMessage.append("but was:<");
		for (String proposal : actual) {
			errorMessage.append(proposal);
			errorMessage.append(" ");
		}
		errorMessage.append(">");
		
		return errorMessage.toString();
	}
}