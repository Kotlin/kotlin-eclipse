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
package org.jetbrains.kotlin.ui.tests.editors.navigation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.ui.PlatformUI;
import org.jetbrains.kotlin.testframework.editor.KotlinEditorAutoTestCase;
import org.jetbrains.kotlin.testframework.editor.TextEditorTest;
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils;
import org.jetbrains.kotlin.testframework.utils.SourceFileData;

import com.intellij.openapi.util.Condition;
import com.intellij.util.containers.ContainerUtil;

public abstract class KotlinNavigationTestCase extends KotlinEditorAutoTestCase {
    
    private static class NavigationSourceFileData extends SourceFileData {
        
        public static final Condition<NavigationSourceFileData> IS_BEFORE_PREDICATE = new Condition<NavigationSourceFileData>() {
            @Override
            public boolean value(NavigationSourceFileData data) {
                return data.isBefore;
            }
            
        };
        
        public static final Condition<NavigationSourceFileData> IS_AFTER_PREDICATE = new Condition<NavigationSourceFileData>() {
            @Override
            public boolean value(NavigationSourceFileData data) {
                return data.isAfter;
            }
            
        };
        
        private boolean isBefore = false;
        private boolean isAfter = false;
        
        public NavigationSourceFileData(File file) throws IOException {
            super(file);
        }
        
        public static List<NavigationSourceFileData> getTestFiles(File testFolder) {
            List<NavigationSourceFileData> result = new ArrayList<NavigationSourceFileData>();
            
            for (File file : testFolder.listFiles()) {
            	String fileName = file.getName();

            	NavigationSourceFileData data;
				try {
					data = new NavigationSourceFileData(file);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				
            	result.add(data);

            	if (fileName.endsWith(BEFORE_FILE_EXTENSION)) {
            		data.isBefore = true;
            	}
            	if (fileName.endsWith(AFTER_FILE_EXTENSION)) {
            		data.isAfter = true;
            	}
            }
            
            return result;
        }
        
        public static NavigationSourceFileData getFileByPredicate(Iterable<NavigationSourceFileData> files,
                Condition<NavigationSourceFileData> predicate) {
            return ContainerUtil.find(files, predicate);
        }
    }
    
    private static final String NAVIGATION_TEST_DATA_PATH = "navigation/";
    
    private void performTest(String contentAfter) {
        testEditor.accelerateOpenDeclaration();
        
        JavaEditor activeEditor = (JavaEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
        EditorTestUtils.assertByEditor(activeEditor, contentAfter);
    }
    
    @Override
    protected void doSingleFileAutoTest(String testPath) {
        String fileText = getText(testPath);
        testEditor = configureEditor(
                getNameByPath(testPath),
                fileText,
                TextEditorTest.TEST_PROJECT_NAME,
                NavigationSourceFileData.getPackageFromContent(fileText));
        
        performTest(getText(testPath + AFTER_FILE_EXTENSION));
    }
    
    @Override
    protected void doMultiFileAutoTest(File testFolder) {
        Collection<NavigationSourceFileData> files = NavigationSourceFileData.getTestFiles(testFolder);
        
        NavigationSourceFileData target = NavigationSourceFileData.getFileByPredicate(
                files,
                NavigationSourceFileData.IS_BEFORE_PREDICATE);
        testEditor = configureEditor(
                target.getFileName().replace(BEFORE_FILE_EXTENSION, ""),
                target.getContent(),
                TextEditorTest.TEST_PROJECT_NAME,
                target.getPackageName());
        
        NavigationSourceFileData targetAfter = NavigationSourceFileData.getFileByPredicate(
                files,
                NavigationSourceFileData.IS_AFTER_PREDICATE);
        for (NavigationSourceFileData data : files) {
            if (data != target) {
                String fileName = data.getFileName();
                
                createSourceFile(
                        data.getPackageName(),
                        data != targetAfter ? fileName : fileName.replace(AFTER_FILE_EXTENSION, ""),
                        data.getContent());
            }
        }
        
        performTest(targetAfter.getContent());
    }
    
    @Override
    protected String getTestDataPath() {
        return super.getTestDataPath() + NAVIGATION_TEST_DATA_PATH;
    }
}
