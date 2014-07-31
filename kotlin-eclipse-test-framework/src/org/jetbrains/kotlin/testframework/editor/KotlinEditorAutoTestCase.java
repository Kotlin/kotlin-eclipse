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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.kotlin.testframework.utils.SourceFileData;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

public abstract class KotlinEditorAutoTestCase extends KotlinEditorTestCase {

	protected abstract static class EditorSourceFileData extends SourceFileData {

		public EditorSourceFileData(File file) {
			super(file.getName(), getText(file));
		}
	}
	
	protected static class WithAfterSourceFileData extends EditorSourceFileData {
        
	    private static final Predicate<WithAfterSourceFileData> TARGET_PREDICATE = new Predicate<WithAfterSourceFileData>() {
            @Override
            public boolean apply(WithAfterSourceFileData data) {
                return data.contentAfter != null;
            }
	    };
       
        private static final String NO_TARGET_FILE_FOUND_ERROR_MESSAGE = "No target file found";
        private static final String NO_TARGET_FILE_FOUND_FOR_AFTER_FILE_ERROR_MESSAGE_FORMAT = "No target file found for \'%s\' file";
        
        private String contentAfter = null;
        
        public WithAfterSourceFileData(File file) {
            super(file);
        }
        
        public String getContentAfter() {
            return contentAfter;
        }
        
        public static Collection<WithAfterSourceFileData> getTestFiles(File testFolder) {
            Map<String, WithAfterSourceFileData> result = new HashMap<String, WithAfterSourceFileData>();
            
            File targetAfterFile = null;
            for (File file : testFolder.listFiles()) {
                String fileName = file.getName();
                
                if (!fileName.endsWith(AFTER_FILE_EXTENSION)) {
                    result.put(fileName, new WithAfterSourceFileData(file));
                } else {
                    targetAfterFile = file;
                }
            }
            
            if (targetAfterFile == null) {
                throw new RuntimeException(NO_TARGET_FILE_FOUND_ERROR_MESSAGE);
            }
            
            WithAfterSourceFileData target = result.get(targetAfterFile.getName().replace(AFTER_FILE_EXTENSION, ""));
            if (target == null) {
                throw new RuntimeException(String.format(NO_TARGET_FILE_FOUND_FOR_AFTER_FILE_ERROR_MESSAGE_FORMAT, targetAfterFile.getAbsolutePath()));
            }
            
            target.contentAfter = getText(targetAfterFile);       
            
            return result.values();
        }
        
        public static WithAfterSourceFileData getTargetFile(Iterable<WithAfterSourceFileData> files) {
            return Iterables.<WithAfterSourceFileData>find(files, TARGET_PREDICATE, null);
        }
    }
	
	protected final void doAutoTest() {
		String testPath = getTestDataPath() + name.getMethodName();
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
	
	private static final String TEST_DATA_PATH = "testData/";
	
	protected static final String KT_FILE_EXTENSION = ".kt";
	protected static final String AFTER_FILE_EXTENSION = ".after";
	protected static final String BEFORE_FILE_EXTENSION = ".before";
	
	protected abstract void doSingleFileAutoTest(String testPath);
	
	protected abstract void doMultiFileAutoTest(File testFolder);

	protected String getTestDataPath() {
		return TEST_DATA_PATH;
	}
}
