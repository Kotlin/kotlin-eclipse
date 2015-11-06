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
import java.util.Map;

import kotlin.Pair;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.core.model.KotlinAnalysisProjectCache;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.testframework.editor.KotlinEditorAutoTestCase;
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils;
import org.jetbrains.kotlin.testframework.utils.SourceFileData;
import org.jetbrains.kotlin.ui.editors.annotations.AnnotationManager;
import org.jetbrains.kotlin.ui.editors.annotations.DiagnosticAnnotation;
import org.jetbrains.kotlin.ui.editors.annotations.DiagnosticAnnotationUtil;
import org.junit.Assert;
import org.junit.Before;

import com.google.common.collect.Lists;

public abstract class KotlinAnalyzerInIDETestCase extends KotlinEditorAutoTestCase {
    
    @Before
    public void before() {
        configureProjectWithStdLib();
    }
    
    private static final String ANALYZER_TEST_DATA_PATH_SEGMENT = "ide_analyzer";
    
    private void performTest(IFile file, String expectedFileText, List<DiagnosticAnnotation> annotations) {
        try {
        	file.deleteMarkers(AnnotationManager.MARKER_PROBLEM_TYPE, true, IResource.DEPTH_INFINITE);
        	for (DiagnosticAnnotation annotation : annotations) {
        		AnnotationManager.INSTANCE.addProblemMarker(annotation, file);
        	}
        	
            IMarker[] markers = file.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
            String actual = insertTagsForErrors(loadEclipseFile(file), markers);
            
            Assert.assertEquals(actual, expectedFileText);
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
		
		KotlinTestUtils.joinBuildThread();
		
		IJavaProject javaProject = getTestProject().getJavaProject();
		BindingContext bindingContext = KotlinAnalysisProjectCache.INSTANCE.getAnalysisResult(javaProject).getBindingContext();
		Map<IFile, List<DiagnosticAnnotation>> annotations = DiagnosticAnnotationUtil.INSTANCE.handleDiagnostics(bindingContext.getDiagnostics());
		
		for (Pair<IFile, String> fileAndExpectedData : filesWithExpectedData) {
			IFile file = fileAndExpectedData.getFirst();
			if (annotations.containsKey(file)) {
				performTest(
						file, 
						fileAndExpectedData.getSecond(), 
						annotations.get(file));
			}
		}
    }
    
    private List<Pair<IFile, String>> loadFilesToProject(@NotNull List<File> files) {
        List<Pair<IFile, String>> filesWithExpectedData = Lists.newArrayList(); 
        for (File file : files) {
            String input = KotlinTestUtils.getText(file.getAbsolutePath());
            String resolvedInput = KotlinTestUtils.resolveTestTags(input);
            filesWithExpectedData.add(new Pair<IFile, String>(
                    createSourceFile(SourceFileData.getPackageFromContent(resolvedInput), file.getName(), 
                            KotlinTestUtils.resolveTestTags(resolvedInput)), 
                    input));
        }
        
        return filesWithExpectedData;
    }
    
    private String loadEclipseFile(IFile file) {
        return KotlinTestUtils.getText(file.getLocation().toOSString());
    }
    
    private static String insertTagsForErrors(String fileText, IMarker[] markers) throws CoreException {
        StringBuilder result = new StringBuilder(fileText);
        
        Integer offset = 0;
        for (IMarker marker : markers) {
        	if (!(marker.getAttribute(IMarker.SEVERITY, 0) == IMarker.SEVERITY_ERROR)) {
        		continue;
        	}
        	
        	offset += insertTagByOffset(result, KotlinTestUtils.ERROR_TAG_OPEN, getTagStartOffset(marker, IMarker.CHAR_START), offset);
        	offset += insertTagByOffset(result, KotlinTestUtils.ERROR_TAG_CLOSE, getTagStartOffset(marker, IMarker.CHAR_END), offset);
        }
        
        return result.toString();
    }
    
    private static int getTagStartOffset(IMarker marker, String type) throws CoreException {
        return (int) marker.getAttribute(type);
    }
    
    private static int insertTagByOffset(StringBuilder builder, String tag, int tagStartOffset, int offset) {
    	int tagOffset = tagStartOffset + offset;
		builder.insert(tagOffset, tag);
        return tag.length();
    }
    
    @Override
    protected String getTestDataRelativePath() {
        return ANALYZER_TEST_DATA_PATH_SEGMENT;
    }
}
