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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.IType;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.TextInvocationContext;
import org.eclipse.ui.IMarkerResolution;
import org.jetbrains.kotlin.testframework.editor.KotlinEditorAutoTestCase;
import org.jetbrains.kotlin.testframework.editor.TextEditorTest;
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils;
import org.jetbrains.kotlin.testframework.utils.ExpectedCompletionUtils;
import org.jetbrains.kotlin.ui.editors.KotlinCorrectionProcessor;
import org.jetbrains.kotlin.ui.editors.quickfix.AutoImportMarkerResolution;
import org.jetbrains.kotlin.ui.editors.quickfix.KotlinMarkerResolutionProposal;
import org.junit.Assert;

public abstract class KotlinAutoImportTestCase extends KotlinEditorAutoTestCase {
	
	private static class AutoimportSourceFileData extends SourceFileData {
		
		private static final String NO_TARGET_FILE_FOUND_ERROR_MESSAGE = "No target file found";
		private static final String NO_TARGET_FILE_FOUND_FOR_AFTER_FILE_ERROR_MESSAGE_FORMAT = "No target file found for \'%s\' file";
		
		private String contentAfter = null;

		public AutoimportSourceFileData(File file) {
			super(file);
		}
		
		public String getContentAfter() {
			return contentAfter;
		}
		
		public void setContentAfter(String contentAfter) {
			this.contentAfter = contentAfter;
		}
		
		public boolean isTarget() {
			return contentAfter != null;
		}
		
		public static Collection<AutoimportSourceFileData> getTestFiles(File testFolder) {
			Map<String, AutoimportSourceFileData> result = new HashMap<String, AutoimportSourceFileData>();
			
			File targetAfterFile = null;
			for (File file : testFolder.listFiles()) {
				String fileName = file.getName();
				
				if (!fileName.endsWith(AFTER_FILE_EXTENSION)) {
					result.put(fileName, new AutoimportSourceFileData(file));
				} else {
					targetAfterFile = file;
				}
			}
			
			if (targetAfterFile == null) {
				throw new RuntimeException(NO_TARGET_FILE_FOUND_ERROR_MESSAGE);
			}
			
			AutoimportSourceFileData target = result.get(targetAfterFile.getName().replace(AFTER_FILE_EXTENSION, ""));
			if (target == null) {
				throw new RuntimeException(String.format(NO_TARGET_FILE_FOUND_FOR_AFTER_FILE_ERROR_MESSAGE_FORMAT, targetAfterFile.getAbsolutePath()));
			}
			
			target.setContentAfter(getText(targetAfterFile));		
			
			return result.values();
		}
		
		public static AutoimportSourceFileData getTargetFile(Collection<AutoimportSourceFileData> files) {
			for (AutoimportSourceFileData data : files) {
				if (data.isTarget()) {
					return data;
				}
			}
			
			return null;
		}
	}
	
	private static final String AUTOIMPORT_TEST_DATA_PATH = "completion/autoimport/";
	
	private static final String COUNT_ASSERTION_ERROR_MESSAGE = "Number of actual proposals differs from the number of expected proposals";
	private static final String EXISTENCE_ASSERTION_ERROR_MESSAGE_FORMAT = "List of actual proposals doesn't contain expected proposal: %s";
	
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
	
	@Override
	protected void doSingleFileAutoTest(String testPath) {
		String fileText = getText(testPath);
		testEditor = configureEditor(getNameByPath(testPath), fileText, TextEditorTest.TEST_PROJECT_NAME, AutoimportSourceFileData.getPackageFromContent(fileText));

		performTest(fileText, getText(testPath + AFTER_FILE_EXTENSION));
	}
	
	@Override
	protected void doMultiFileAutoTest(File testFolder) {
		Collection<AutoimportSourceFileData> files = AutoimportSourceFileData.getTestFiles(testFolder);
		
		AutoimportSourceFileData target = AutoimportSourceFileData.getTargetFile(files);
		testEditor = configureEditor(target.getFileName(), target.getContent(), TextEditorTest.TEST_PROJECT_NAME, target.getPackageName());
		
		for (AutoimportSourceFileData data : files) {
			if (!data.isTarget()) {
				createSourceFile(data.getPackageName(), data.getFileName(), data.getContent());
			}
		}
		
		performTest(target.getContent(), target.getContentAfter());
	}
	
	@Override
	protected String getTestDataPath() {
		return super.getTestDataPath() + AUTOIMPORT_TEST_DATA_PATH;
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
