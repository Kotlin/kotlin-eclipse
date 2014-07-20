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
package org.jetbrains.kotlin.ui.tests.editors.quickfix.autoimport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.TextInvocationContext;
import org.eclipse.ui.IMarkerResolution;
import org.jetbrains.kotlin.testframework.editor.KotlinEditorTestCase;
import org.jetbrains.kotlin.testframework.editor.TextEditorTest;
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils;
import org.jetbrains.kotlin.testframework.utils.ExpectedCompletionUtils;
import org.jetbrains.kotlin.ui.editors.KotlinCorrectionProcessor;
import org.jetbrains.kotlin.ui.editors.quickfix.AutoImportMarkerResolution;
import org.jetbrains.kotlin.ui.editors.quickfix.KotlinMarkerResolutionProposal;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.rules.TestName;

public abstract class KotlinAutoImportTestCase extends KotlinEditorTestCase {

	private static final String KT_FILE_EXTENSION = ".kt";
	private static final String AFTER_FILE_EXTENSION = ".after";
	private static final String TEST_DATA_PROJECT_RELATIVE = "testData/completion/autoimport/";
	
	private static final String COUNT_ASSERTION_ERROR_MESSAGE = "Number of actual proposals differs from the number of expected proposals";
	private static final String EXISTENCE_ASSERTION_ERROR_MESSAGE_FORMAT = "List of actual proposals doesn't contain expected proposal: %s";

	@Rule
	public TestName name = new TestName();
	
	private String getTestNameProjectRelative() {
		return TEST_DATA_PROJECT_RELATIVE + name.getMethodName();
	}
	
	private JavaEditor getEditor() {
		return testEditor.getEditor();
	}
	
	protected void doTest() {
		doTestFor(getTestNameProjectRelative() + KT_FILE_EXTENSION);
	}
	
	private void performTest(String fileText, String testPath) {
		joinBuildThread();
		
		List<ICompletionProposal> proposals = createProposals();
		assertCount(proposals, fileText);
		assertExistence(proposals, fileText);
		
		if (!proposals.isEmpty()) {
			proposals.get(0).apply(getEditor().getViewer().getDocument());
		}
		
		EditorTestUtils.assertByEditor(getEditor(), getText(testPath + AFTER_FILE_EXTENSION));
	}

	private void doTestFor(String testPath) {
		String fileText = getText(testPath);
		testEditor = configureEditor(getNameByPath(testPath), fileText, TextEditorTest.TEST_PROJECT_NAME, TextEditorTest.TEST_PACKAGE_NAME);

		performTest(fileText, testPath);
	}
	
	protected void doMultifileTest(int targetFileNumber, String... files) {
		doMultifileTestFor(getTestNameProjectRelative(), targetFileNumber, files);
	}
	
	private void doMultifileTestFor(String testFolderPath, int targetFileNumber, String[] files) {
		List<String> fileNames = getProjectRelativeFileNames(files);
		List<String> packageNames = getPackageNames(files);
		List<String> contents = getContents(fileNames);
		
		testEditor = configureEditor(getNameByPath(files[targetFileNumber]), contents.get(targetFileNumber), TextEditorTest.TEST_PROJECT_NAME, packageNames.get(targetFileNumber));				
		for (int i = 0; i < files.length; i++) {
			if (i != targetFileNumber) {
				createSourceFile(packageNames.get(i), getNameByPath(files[i]), contents.get(i));
			}
		}
		
		performTest(contents.get(targetFileNumber), fileNames.get(targetFileNumber));
	}
	
	private List<String> getProjectRelativeFileNames(String[] fileNames) {
		List<String> result = new ArrayList<String>();
		
		for (String fileName : fileNames) {
			result.add(getTestNameProjectRelative() + "/" + fileName);
		}
		
		return result;
	}
	
	private static List<String> getPackageNames(String[] fileNames) {
		List<String> result = new ArrayList<String>();
		
		for (String fileName : fileNames) {
			result.add(getPackageName(fileName));
		}
		
		return result;
	}
	
	private static List<String> getContents(List<String> fileNames) {
		List<String> result = new ArrayList<String>();
		
		for (String fileName : fileNames) {
			result.add(getText(fileName));
		}
		
		return result;
	}
	
	private static String getPackageName(String projectRelativeTestPath) {
		return new Path(projectRelativeTestPath).removeLastSegments(1).toString().replaceAll("/", ".");
	}
	
	private List<ICompletionProposal> createProposals() {
		return Arrays.asList(new KotlinCorrectionProcessor(getEditor()).computeQuickAssistProposals(new TextInvocationContext(getEditor().getViewer(), getCaret(), -1)));
	}
	
	private static void assertCount(List<ICompletionProposal> proposals, String fileText) {
		Integer expectedNumber = ExpectedCompletionUtils.numberOfItemsShouldPresent(fileText);
		
		if (expectedNumber != null) {
			Assert.assertEquals(COUNT_ASSERTION_ERROR_MESSAGE, expectedNumber.intValue(), proposals.size());
		}
	}

	private static void assertExistence(List<ICompletionProposal> proposals, String fileText) {
		List<String> expectedStrings = ExpectedCompletionUtils.itemsShouldExist(fileText);
		List<String> actualStrings = getProposalsStrings(proposals);
		
		for (String string : expectedStrings) {
			Assert.assertTrue(String.format(EXISTENCE_ASSERTION_ERROR_MESSAGE_FORMAT, string), actualStrings.contains(string));
		}		
	}
	
	private static List<String> getProposalsStrings(List<ICompletionProposal> proposals) {
		List<String> result = new ArrayList<String>();
		
		for (ICompletionProposal proposal : proposals) {
			IMarkerResolution resolution = ((KotlinMarkerResolutionProposal) proposal).getMarkerResolution();
			IType type = ((AutoImportMarkerResolution) resolution).getType();

			result.add(type.getFullyQualifiedName('.'));
		}
		
		return result;
	}
}
