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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import kotlin.collections.CollectionsKt;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.jetbrains.kotlin.testframework.editor.KotlinProjectTestCase;
import org.jetbrains.kotlin.testframework.editor.TextEditorTest;
import org.jetbrains.kotlin.testframework.utils.ExpectedCompletionUtils;
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils;
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor;
import org.jetbrains.kotlin.ui.editors.codeassist.KotlinCompletionProcessor;
import org.jetbrains.kotlin.ui.editors.codeassist.KotlinCompletionProposal;
import org.junit.Assert;
import org.junit.Before;

public abstract class KotlinBasicCompletionTestCase extends KotlinProjectTestCase {
	@Before
	public void configure() {
		configureProject();
	}

	protected void doTest(String testPath) {
		String fileText = KotlinTestUtils.getText(testPath);
		
		boolean shouldHideNonVisible = ExpectedCompletionUtils.shouldHideNonVisibleMembers(fileText);
		JavaPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.CODEASSIST_SHOW_VISIBLE_PROPOSALS, shouldHideNonVisible);
		
		TextEditorTest testEditor = configureEditor(KotlinTestUtils.getNameByPath(testPath), fileText);

		List<String> actualProposals = getActualProposals((KotlinFileEditor) testEditor.getEditor());

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
	
	private List<String> getActualProposals(KotlinFileEditor javaEditor) {
		KotlinCompletionProcessor ktCompletionProcessor = new KotlinCompletionProcessor(javaEditor, null);
		ICompletionProposal[] proposals = ktCompletionProcessor.computeCompletionProposals(
				javaEditor.getViewer(), 
				KotlinTestUtils.getCaret(javaEditor));
		
		List<String> actualProposals = new ArrayList<String>();
		for (ICompletionProposal proposal : proposals) {
		    String replacementString;
		    if (proposal instanceof KotlinCompletionProposal) {
		        replacementString = ((KotlinCompletionProposal) proposal).getReplacementString();
		    } else {
		        replacementString = proposal.getAdditionalProposalInfo();
		    }
		    
			if (replacementString == null) replacementString = proposal.getDisplayString();

			actualProposals.add(replacementString);
		}
		
		return actualProposals;
	}

	private void assertExists(List<String> itemsShouldExist, Set<String> actualItems) {
		Set<String> missing = new HashSet<String>();
		for (String itemShouldExist : itemsShouldExist) {
			if (!actualItems.contains(itemShouldExist.trim())) {
				missing.add(itemShouldExist);
			}
		}
		
		if (!missing.isEmpty()) {
			Assert.fail(getErrorMessage("Items not found.", itemsShouldExist, actualItems, missing, Collections.<String>emptySet()));
		}
	}
	
	private void assertNotExists(List<String> itemsShouldAbsent, Set<String> actualItems) {
		Set<String> added = new HashSet<String>();
		for (String itemShouldAbsent : itemsShouldAbsent) {
			if (actualItems.contains(itemShouldAbsent)) {
				added.add(itemShouldAbsent);
			}
		}
		
		if (!added.isEmpty()) {
			Assert.fail(getErrorMessage("Items must be absent.", itemsShouldAbsent, actualItems, Collections.<String>emptySet(), added));
		}
	}
	
	private String getErrorMessage(String message, List<String> expected, Set<String> actual, Set<String> missing, Set<String> added) {
		StringBuilder errorMessage = new StringBuilder();
		
		errorMessage.append(message).append("\n");
		errorMessage.append("Expected: <\n");
		for (String proposal : CollectionsKt.sorted(expected)) {
			if (missing.contains(proposal)) {
				errorMessage.append("-");
			}
			
			errorMessage.append(proposal);
			errorMessage.append("\n");
		}
		errorMessage.append("> ");
		
		errorMessage.append("but was:<\n");
		for (String proposal : CollectionsKt.sorted(actual)) {
			if (added.contains(proposal)) {
				errorMessage.append("+");
			}
			
			errorMessage.append(proposal);
			errorMessage.append("\n");
		}
		errorMessage.append(">");
		
		return errorMessage.toString();
	}
}