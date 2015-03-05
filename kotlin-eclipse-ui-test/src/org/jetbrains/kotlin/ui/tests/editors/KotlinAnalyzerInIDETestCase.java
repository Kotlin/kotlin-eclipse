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
package org.jetbrains.kotlin.ui.tests.editors;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import kotlin.Pair;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.testframework.editor.KotlinEditorAutoTestCase;
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils;
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils;
import org.jetbrains.kotlin.testframework.utils.SourceFileData;

import com.google.common.collect.Lists;

public abstract class KotlinAnalyzerInIDETestCase extends KotlinEditorAutoTestCase {
    
    private static final String ANALYZER_TEST_DATA_PATH_SEGMENT = "ide_analyzer";
    
    private void performTest(IFile file, String expectedFileText) {
        try {
            configureProjectWithStdLibAndBuilder();
            file.touch(null);
            
            testEditor.getEclipseProject().build(IncrementalProjectBuilder.FULL_BUILD, null);
            KotlinTestUtils.joinBuildThread();
            
            IMarker[] markers = file.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
            String actual = insertTagsForErrors(loadEclipseFile(file), markers);
            
            EditorTestUtils.assertByStringWithOffset(actual, expectedFileText);
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    protected void doSingleFileAutoTest(String testPath) {
        loadFilesToProjectAndDoTest(Collections.singletonList(new File(testPath)));
    }
    
    @Override
    protected void doMultiFileAutoTest(File testFolder) {
        loadFilesToProjectAndDoTest(Arrays.asList(testFolder.listFiles()));
    }
    
    private void loadFilesToProjectAndDoTest(@NotNull List<File> files) {
        List<Pair<IFile, String>> filesWithExpectedData = loadFilesToProject(files);
        for (Pair<IFile, String> fileAndExpectedData : filesWithExpectedData) {
            performTest(fileAndExpectedData.getFirst(), fileAndExpectedData.getSecond());
        }
    }
    
    private List<Pair<IFile, String>> loadFilesToProject(@NotNull List<File> files) {
        List<Pair<IFile, String>> filesWithExpectedData = Lists.newArrayList(); 
        for (File file : files) {
            String input = getText(file);
            String resolvedInput = resolveTestTags(input);
            filesWithExpectedData.add(new Pair<IFile, String>(
                    createSourceFile(SourceFileData.getPackageFromContent(resolvedInput), file.getName(), 
                            resolveTestTags(resolvedInput)), 
                    input));
        }
        
        return filesWithExpectedData;
    }
    
    private String loadEclipseFile(IFile file) {
        return getText(file.getLocation().toFile());
    }
    
    private String insertTagsForErrors(String text, IMarker[] markers) throws CoreException {
        StringBuilder editorInput = new StringBuilder(text);
        int tagShift = 0;
        for (IMarker marker : markers) {
            if (marker.getAttribute(IMarker.SEVERITY, 0) == IMarker.SEVERITY_ERROR) {
                editorInput.insert((int) marker.getAttribute(IMarker.CHAR_START) + tagShift,
                        KotlinTestUtils.ERROR_TAG_OPEN);
                tagShift += KotlinTestUtils.ERROR_TAG_OPEN.length();
                
                editorInput.insert((int) marker.getAttribute(IMarker.CHAR_END) + tagShift,
                        KotlinTestUtils.ERROR_TAG_CLOSE);
                tagShift += KotlinTestUtils.ERROR_TAG_CLOSE.length();
            }
        }
        
        return editorInput.toString();
    }
    
    @Override
    protected String getTestDataRelativePath() {
        return ANALYZER_TEST_DATA_PATH_SEGMENT;
    }
    
    
    private void configureProjectWithStdLibAndBuilder() {
        try {
            KotlinTestUtils.addKotlinBuilder(testEditor.getEclipseProject());
            testEditor.getTestJavaProject().addKotlinRuntime();
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }
}
