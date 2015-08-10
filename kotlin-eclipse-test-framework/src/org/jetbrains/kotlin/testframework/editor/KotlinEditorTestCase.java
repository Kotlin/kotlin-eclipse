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
package org.jetbrains.kotlin.testframework.editor;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.jetbrains.kotlin.testframework.utils.TestJavaProject;
import org.jetbrains.kotlin.testframework.utils.WorkspaceUtil;
import org.jetbrains.kotlin.ui.editors.KotlinEditor;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

import com.intellij.openapi.util.io.FileUtil;

public abstract class KotlinEditorTestCase {
    
    public enum Separator {
        TAB, SPACE;
    }
    
    public static final String CARET_TAG = "<caret>";
    public static final String SELECTION_TAG_OPEN = "<selection>";
    public static final String SELECTION_TAG_CLOSE = "</selection>";
    public static final String ERROR_TAG_OPEN = "<error>";
    public static final String ERROR_TAG_CLOSE = "</error>";
    public static final String WARNING_TAG_OPEN = "<warning>";
    public static final String WARNING_TAG_CLOSE = "</warning>";
    public static final String BREAK_TAG = "<br>";
    public static final String REFERENCE_TAG = "<ref>";
    
    @Rule
    public TestName name = new TestName();
    protected TextEditorTest testEditor;
    private Separator initialSeparator;
    private int initialSpacesCount;
    
    @After
    public void afterTest() {
        deleteProjectAndCloseEditors();
        
        EditorsUI.getPreferenceStore().setValue(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS, (Separator.SPACE == initialSeparator));
        EditorsUI.getPreferenceStore().setValue(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH, initialSpacesCount);
    }
    
    @Before
    public void beforeTest() {
        refreshWorkspace();
        
        initialSeparator = EditorsUI.getPreferenceStore().getBoolean(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS) ? Separator.SPACE : Separator.TAB;
        initialSpacesCount = EditorsUI.getPreferenceStore().getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH);
    }
    
    protected KotlinEditor getEditor() {
        return (KotlinEditor) testEditor.getEditor();
    }
    
    protected int getCaret() {
        return getEditor().getViewer().getTextWidget().getCaretOffset();
    }
    
    public IFile createSourceFile(String pkg, String fileName, String content) {
        content = removeTags(content);
        try {
            if (testEditor == null) {
                testEditor = new TextEditorTest(TextEditorTest.TEST_PROJECT_NAME);
            }
            return testEditor.getTestJavaProject().createSourceFile(pkg, fileName, content);
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }
    
    public IFile createSourceFile(String fileName, String content) {
        return createSourceFile(TextEditorTest.TEST_PACKAGE_NAME, fileName, content);
    }
    
    protected static TextEditorTest configureEditor(String fileName, String content) {
        return configureEditor(fileName, content, TextEditorTest.TEST_PACKAGE_NAME);
    }
    
    protected static TextEditorTest configureEditor(String fileName, String content, String packageName) {
        return configureEditor(fileName, content, TextEditorTest.TEST_PROJECT_NAME, packageName);
    }
    
    protected static TextEditorTest configureEditor(String fileName, String content, String projectName, String packageName) {
        TextEditorTest testEditor = new TextEditorTest(projectName);
        testEditor.createEditor(fileName, resolveTestTags(content), packageName);
        
        return testEditor;
    }
    
    private void deleteProjectAndCloseEditors() {
        try {
            joinBuildThread();
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeAllEditors(false);
            
            if (testEditor != null) {
            	TestJavaProject testJavaProject = testEditor.getTestJavaProject();
            	if (testJavaProject != null) {
            		testJavaProject.clean();
            	}
            }
            
            IProject projects[] = ResourcesPlugin.getWorkspace().getRoot().getProjects();
            for (IProject project : projects) {
                project.delete(true, true, null);
            }
        } catch (CoreException e) {
            e.printStackTrace();
        }
        
    }
    
    public static void refreshWorkspace() {
        WorkspaceUtil.refreshWorkspace();
        try {
            Job.getJobManager().join(ResourcesPlugin.FAMILY_MANUAL_REFRESH, new NullProgressMonitor());
        } catch (OperationCanceledException | InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    public static String getText(String testPath) {
        return getText(new File(testPath));
    }
    
    public static String getText(File file) {
        try {
            return String.valueOf(FileUtil.loadFile(file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static void joinBuildThread() {
        while (true) {
            try {
            	Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
                break;
            } catch (OperationCanceledException | InterruptedException e) {
            }
        }
    }
    
    public static String resolveTestTags(String text) {
        return text.replaceAll(ERROR_TAG_OPEN, "")
                .replaceAll(ERROR_TAG_CLOSE, "")
                .replaceAll(WARNING_TAG_OPEN, "")
                .replaceAll(WARNING_TAG_CLOSE, "")
                .replaceAll(BREAK_TAG, System.lineSeparator());
    }
    
    public static String removeTags(String text) {
        return resolveTestTags(text).replaceAll(CARET_TAG, "")
        		.replaceAll(SELECTION_TAG_OPEN, "")
        		.replaceAll(SELECTION_TAG_CLOSE, "")
        		.replaceAll(REFERENCE_TAG, "");
    }
    
    public static String getNameByPath(String testPath) {
        return new Path(testPath).lastSegment();
    }
}