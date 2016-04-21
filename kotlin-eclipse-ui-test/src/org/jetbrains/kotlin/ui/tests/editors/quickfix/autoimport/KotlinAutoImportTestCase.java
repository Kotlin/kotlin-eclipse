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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.TextInvocationContext;
import org.jetbrains.kotlin.testframework.editor.KotlinEditorWithAfterFileTestCase;
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils;
import org.jetbrains.kotlin.testframework.utils.ExpectedCompletionUtils;
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils;
import org.jetbrains.kotlin.ui.editors.KotlinCorrectionProcessor;
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor;
import org.jetbrains.kotlin.ui.editors.quickfix.KotlinMarkerResolution;
import org.junit.Assert;
import org.junit.Before;

public abstract class KotlinAutoImportTestCase extends KotlinEditorWithAfterFileTestCase {
    
    private static final String AUTOIMPORT_TEST_DATA_PATH_SEGMENT = "completion/autoimport";
    
    private static final String COUNT_ASSERTION_ERROR_MESSAGE = "Number of actual proposals differs from the number of expected proposals";
    private static final String EXISTENCE_ASSERTION_ERROR_MESSAGE_FORMAT = "List of actual proposals doesn't contain expected proposal: %s";
    
    @Before
    public void before() {
        configureProjectWithStdLib();
    }
    
    private List<KotlinMarkerResolution> createProposals() {
        return KotlinQuickFixTestCaseKt.getProposals(getTestEditor());
    }
    
    @Override
    protected void performTest(String fileText, String content) {
        List<KotlinMarkerResolution> proposals = createProposals();
        assertCount(proposals, fileText);
        assertExistence(proposals, fileText);
        
        if (!proposals.isEmpty()) {
            proposals.get(0).apply(getTestEditor().getEditingFile());;
        }
        
        EditorTestUtils.assertByEditor(getEditor(), content);
    }
    
    @Override
    protected String getTestDataRelativePath() {
        return AUTOIMPORT_TEST_DATA_PATH_SEGMENT;
    }
    
    private static void assertCount(List<KotlinMarkerResolution> proposals, String fileText) {
        Integer expectedNumber = ExpectedCompletionUtils.numberOfItemsShouldPresent(fileText);
        
        if (expectedNumber != null) {
            Assert.assertEquals(COUNT_ASSERTION_ERROR_MESSAGE, expectedNumber.intValue(), proposals.size());
        }
    }
    
    private static void assertExistence(List<KotlinMarkerResolution> proposals, String fileText) {
        List<String> expectedStrings = ExpectedCompletionUtils.itemsShouldExist(fileText);
        List<String> actualStrings = getProposalsStrings(proposals);
        
        for (String string : expectedStrings) {
            Assert.assertTrue(String.format(EXISTENCE_ASSERTION_ERROR_MESSAGE_FORMAT, string), actualStrings.contains(string));
        }
    }
    
    private static List<String> getProposalsStrings(List<KotlinMarkerResolution> proposals) {
        List<String> result = new ArrayList<String>();
        
        for (KotlinMarkerResolution proposal : proposals) {
            result.add(proposal.getLabel());
        }
        
        return result;
    }
    
    private JavaEditor getEditor() {
        return getTestEditor().getEditor();
    }
}
