/*******************************************************************************
* Copyright 2000-2015 JetBrains s.r.o.
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
package org.jetbrains.kotlin.ui.tests.editors

import java.io.File
import java.util.Arrays
import java.util.Collections
import kotlin.Pair
import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IMarker
import org.eclipse.core.resources.IResource
import org.eclipse.core.runtime.CoreException
import org.eclipse.jdt.core.IJavaProject
import org.jetbrains.kotlin.core.model.KotlinAnalysisProjectCache
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.testframework.editor.KotlinEditorAutoTestCase
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils
import org.jetbrains.kotlin.testframework.utils.SourceFileData
import org.jetbrains.kotlin.ui.editors.annotations.AnnotationManager
import org.jetbrains.kotlin.ui.editors.annotations.DiagnosticAnnotation
import org.jetbrains.kotlin.ui.editors.annotations.DiagnosticAnnotationUtil
import org.junit.Assert
import org.junit.Before
import com.google.common.collect.Lists
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.core.ICompilationUnit

abstract class KotlinAnalyzerInIDETestCase : KotlinEditorAutoTestCase() {
    @Before
    fun before() {
        configureProjectWithStdLib()
    }
    
    private fun performTest(file: IFile, expectedFileText: String) {
        val markers = file.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE)
        val actual = insertTagsForErrors(loadEclipseFile(file), markers)
        Assert.assertEquals(expectedFileText, actual)
    }
    
    override fun doSingleFileAutoTest(testPath: String) {
        loadFilesToProjectAndDoTest(Collections.singletonList(File(testPath)))
    }
    
    override fun doMultiFileAutoTest(testFolder: File) {
        loadFilesToProjectAndDoTest(testFolder.listFiles().toList())
    }
    
    private fun loadFilesToProjectAndDoTest(files: List<File>) {
        val filesWithExpectedData = loadFilesToProject(files)
        
        KotlinTestUtils.joinBuildThread()
        
        for (fileAndExpectedData in filesWithExpectedData) {
            val file = fileAndExpectedData.first
            performTest(file, fileAndExpectedData.second)
        }
    }
    
    private fun loadFilesToProject(files: List<File>): List<Pair<IFile, String>> {
        return files.map { file ->
            val input = KotlinTestUtils.getText(file.getAbsolutePath())
            val resolvedInput = KotlinTestUtils.resolveTestTags(input)
            Pair(
                createSourceFile(
                    SourceFileData.getPackageFromContent(resolvedInput), 
                    file.getName(),
                    KotlinTestUtils.resolveTestTags(resolvedInput)),
                input)
        }
    }
    
    private fun loadEclipseFile(file: IFile): String = EditorUtil.getDocument(file).get()
    
    override fun getTestDataRelativePath() = ANALYZER_TEST_DATA_PATH_SEGMENT
}

private val ANALYZER_TEST_DATA_PATH_SEGMENT = "ide_analyzer"

private fun insertTagsForErrors(fileText: String, markers: Array<IMarker>): String {
    val result = StringBuilder(fileText)
    var offset = 0
    for (marker in markers) {
        if (!(marker.getAttribute(IMarker.SEVERITY, 0) === IMarker.SEVERITY_ERROR)) {
            continue
        }
        offset += insertTagByOffset(result, KotlinTestUtils.ERROR_TAG_OPEN, getTagStartOffset(marker, IMarker.CHAR_START), offset.toInt())
        offset += insertTagByOffset(result, KotlinTestUtils.ERROR_TAG_CLOSE, getTagStartOffset(marker, IMarker.CHAR_END), offset.toInt())
    }
    
    return result.toString()
}

private fun getTagStartOffset(marker: IMarker, type: String): Int = marker.getAttribute(type) as Int

private fun insertTagByOffset(builder: StringBuilder, tag: String, tagStartOffset: Int, offset: Int): Int {
    val tagOffset = tagStartOffset + offset
    builder.insert(tagOffset, tag)
    return tag.length
}