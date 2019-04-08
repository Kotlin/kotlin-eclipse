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

import com.intellij.openapi.util.Condition
import com.intellij.util.containers.ContainerUtil
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.core.model.KotlinAnalysisFileCache
import org.jetbrains.kotlin.core.model.KotlinAnalysisProjectCache

import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils
import org.jetbrains.kotlin.testframework.utils.SourceFileData

import java.io.File
import java.io.IOException

abstract class KotlinEditorWithAfterFileTestCase: KotlinEditorAutoTestCase() {

    companion object {
        private const val NO_TARGET_FILE_FOUND_ERROR_MESSAGE = "No target file found"
        private const val NO_TARGET_FILE_FOUND_FOR_AFTER_FILE_ERROR_MESSAGE_FORMAT = "No target file found for \'%s\' file"
    }

    enum class AfterSuffixPosition {
        BEFORE_DOT, AFTER_NAME
    }

    protected open fun getAfterPosition(): AfterSuffixPosition =
        AfterSuffixPosition.AFTER_NAME

    private class WithAfterSourceFileData(file: File) : EditorSourceFileData(file) {

        var contentAfter: String? = null
        private set

        companion object {

            private val TARGET_PREDICATE =
                Condition<WithAfterSourceFileData> { data -> data.contentAfter != null }

            fun getTargetFile(files: Iterable<WithAfterSourceFileData>): WithAfterSourceFileData? =
                ContainerUtil.find(files, TARGET_PREDICATE)

            fun getTestFiles(testFolder: File): MutableCollection<WithAfterSourceFileData> {
                val result = HashMap<String, WithAfterSourceFileData>()

                var targetAfterFile: File? = null
                testFolder.listFiles().forEach { file ->
                    if (!file.name.endsWith(AFTER_FILE_EXTENSION)) {
                        result[file.name] = WithAfterSourceFileData(file)
                    } else {
                        targetAfterFile = file
                    }
                }

                targetAfterFile?.let {
                    result[it.name.replace(AFTER_FILE_EXTENSION, "")]
                        ?.apply {
                            contentAfter = KotlinTestUtils.getText(it.absolutePath)
                        } ?: throw RuntimeException(String.format(NO_TARGET_FILE_FOUND_FOR_AFTER_FILE_ERROR_MESSAGE_FORMAT, it.absolutePath))

                } ?: throw RuntimeException(NO_TARGET_FILE_FOUND_ERROR_MESSAGE)

                return result.values
            }
        }
    }

    protected lateinit var testEditor: TextEditorTest

    protected abstract fun performTest(fileText: String, expectedFileText: String)

    protected open fun loadFilesBeforeOpeningEditor() = false

    private fun waitForCache() {
        runBlocking {
            //KotlinAnalysisProjectCache.getAnalysisResult(testProject.javaProject)
            //KotlinAnalysisFileCache.resetCache()
        }
    }

    override fun doSingleFileAutoTest(testPath: String) {
        val fileText = loadEditor(testPath)

        val afterTestPath = if (getAfterPosition() == AfterSuffixPosition.AFTER_NAME) {
            testPath + AFTER_FILE_EXTENSION
        } else {
            testPath.substring(0, testPath.length - extension.length) + AFTER_FILE_EXTENSION + extension
        }
        waitForCache()
        performTest(fileText, KotlinTestUtils.getText(afterTestPath))
    }

    override fun doMultiFileAutoTest(testFolder: File) {
        val files = WithAfterSourceFileData.getTestFiles(testFolder)

        val target = WithAfterSourceFileData.getTargetFile(files)!!

        if (loadFilesBeforeOpeningEditor()) {
            loadFiles(files, target)
            testEditor = configureEditor(target.fileName, target.content, target.packageName)
        } else {
            testEditor = configureEditor(target.fileName, target.content, target.packageName)
            loadFiles(files, target)
        }
        waitForCache()

        performTest(target.content, target.contentAfter!!)
    }

    private fun loadFiles(files: Collection<WithAfterSourceFileData>, target: WithAfterSourceFileData ) =
        files.forEach { file ->
            if (file != target) {
                createSourceFile(file.packageName, file.fileName, file.content)
            }
        }

    override fun doAutoTestWithDependencyFile(mainTestPath: String, dependencyFile: File) {
        lateinit var fileText: String

        if (loadFilesBeforeOpeningEditor()) {
            loadDependencyFile(dependencyFile)
            fileText = loadEditor(mainTestPath)
        } else {
            fileText = loadEditor(mainTestPath)
            loadDependencyFile(dependencyFile)
        }

        waitForCache()
        performTest(fileText, KotlinTestUtils.getText(mainTestPath + AFTER_FILE_EXTENSION))
    }

    private fun loadEditor(mainTestPath: String): String {
        val fileText = KotlinTestUtils.getText(mainTestPath)
        testEditor = configureEditor(KotlinTestUtils.getNameByPath(mainTestPath), fileText,
            SourceFileData.getPackageFromContent(fileText))
        return fileText
    }

    private fun loadDependencyFile(dependencyFile: File) {
        try {
            val dependencySourceFile = SourceFileData(dependencyFile)
            val fileName = dependencySourceFile.fileName
            val dependencyFileName = fileName.substring(0, fileName.indexOf(FILE_DEPENDENCY_SUFFIX)) +
                    "_dependency" + extension
            createSourceFile(dependencySourceFile.packageName, dependencyFileName,
                    dependencySourceFile.content
            )
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}
