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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	
	private static class SourceFileData {

		private final static String DEFAULT_PACKAGE_NAME = "";
		private final static String JAVA_IDENTIFIER_REGEXP = "[_a-zA-Z]\\w*";
		private final static String PACKAGE_REGEXP = "package\\s((" + JAVA_IDENTIFIER_REGEXP + ")(\\." + JAVA_IDENTIFIER_REGEXP + ")*)";
		private final static Pattern PATTERN = Pattern.compile(PACKAGE_REGEXP);

		private final String packageName;
		private final String content;

		public SourceFileData(File file) {
			this.content = getText(file);
			this.packageName = getPackageFromContent(content);
		}

		public String getPackageName() {
			return packageName;
		}

		public String getContent() {
			return content;
		}

		private static String getPackageFromContent(String content) {
			Matcher matcher = PATTERN.matcher(content);
			return matcher.find() ? matcher.group(1) : DEFAULT_PACKAGE_NAME;
		}
	}

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
	
	private List<ICompletionProposal> createProposals() {
		return Arrays.asList(new KotlinCorrectionProcessor(getEditor()).computeQuickAssistProposals(new TextInvocationContext(getEditor().getViewer(), getCaret(), -1)));
	}
	
	private void performTest(String fileText, String content) {
		joinBuildThread();
		
		List<ICompletionProposal> proposals = createProposals();
		assertCount(proposals, fileText);
		assertExistence(proposals, fileText);
		
		if (!proposals.isEmpty()) {
			proposals.get(0).apply(getEditor().getViewer().getDocument());
		}
		
		EditorTestUtils.assertByEditor(getEditor(), content);
	}
	
	protected void doTest() {
		String testPath = getTestNameProjectRelative();
		File testFile = new File(testPath);
		
		if (testFile.exists() && testFile.isDirectory()) {
			doMultiFileTest(testFile);
		} else {
			doSingleFileTest(testPath + KT_FILE_EXTENSION);
		}
	}
	
	private void doSingleFileTest(String testPath) {
		String fileText = getText(testPath);
		testEditor = configureEditor(getNameByPath(testPath), fileText, TextEditorTest.TEST_PROJECT_NAME, TextEditorTest.TEST_PACKAGE_NAME);

		performTest(fileText, getText(testPath + AFTER_FILE_EXTENSION));
	}
	
	private void doMultiFileTest(File testFolder) {
		Map<String, SourceFileData> map = new HashMap<String, SourceFileData>();
		
		String targetFileName = null;
		File targetAfterTestFile = null;
		for (File file : testFolder.listFiles()) {
			String fileName = file.getName();
			
			if (!fileName.endsWith(AFTER_FILE_EXTENSION)) {
				map.put(fileName, new SourceFileData(file));
			} else {
				targetFileName = fileName.replace(AFTER_FILE_EXTENSION, "");
				targetAfterTestFile = file;
			}
		}
		
		SourceFileData target = map.get(targetFileName);
		testEditor = configureEditor(targetFileName, target.getContent(), TextEditorTest.TEST_PROJECT_NAME, target.getPackageName());
		map.remove(targetFileName);
		
		for (Map.Entry<String, SourceFileData> entry : map.entrySet()) {
			SourceFileData data = entry.getValue();
			createSourceFile(data.getPackageName(), entry.getKey(), data.getContent());
		}
		
		performTest(target.getContent(), getText(targetAfterTestFile));
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
