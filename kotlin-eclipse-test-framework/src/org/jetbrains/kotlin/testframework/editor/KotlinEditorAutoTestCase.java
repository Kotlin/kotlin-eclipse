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

import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils;
import org.jetbrains.kotlin.testframework.utils.SourceFileData;
import org.junit.Rule;
import org.junit.rules.TestName;

public abstract class KotlinEditorAutoTestCase extends KotlinProjectTestCase {
    
    protected abstract static class EditorSourceFileData extends SourceFileData {
        public EditorSourceFileData(File file) {
            super(file.getName(), KotlinTestUtils.getText(file.getAbsolutePath()));
        }
    }
    
    @Rule
    public TestName name = new TestName();
    
    private static final String TEST_DATA_PATH = "testData";
    
    protected static final String KT_FILE_EXTENSION = ".kt";
    protected static final String AFTER_FILE_EXTENSION = ".after";
    protected static final String BEFORE_FILE_EXTENSION = ".before";
    
    protected final void doAutoTest() {
        String testPath = TEST_DATA_PATH + "/" + getTestDataRelativePath() + "/" + name.getMethodName();
        File testFolder = new File(testPath);
        File testFile = new File(testPath + KT_FILE_EXTENSION);
        
        if (testFolder.exists() && testFolder.isDirectory()) {
            doMultiFileAutoTest(testFolder);
        } else if (testFile.exists() && testFile.isFile()) {
            doSingleFileAutoTest(testPath + KT_FILE_EXTENSION);
        } else {
            throw new RuntimeException(String.format("Neither file \'%s\' nor directory \'%s\' was found", testFile.getAbsolutePath(), testFolder.getAbsolutePath()));
        }
    }
    
    protected abstract void doSingleFileAutoTest(String testPath);
    
    protected abstract void doMultiFileAutoTest(File testFolder);
    
    protected abstract String getTestDataRelativePath();
}
