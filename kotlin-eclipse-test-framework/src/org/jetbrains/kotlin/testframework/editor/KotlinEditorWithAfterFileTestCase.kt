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
 */
package org.jetbrains.kotlin.testframework.editor

import org.jetbrains.kotlin.testframework.utils.FileReaderHolder
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils
import org.jetbrains.kotlin.testframework.utils.SourceFileData
import java.io.File
import java.io.IOException


abstract class KotlinEditorWithAfterFileTestCase(
    private val afterPosition: AfterSuffixPosition = AfterSuffixPosition.AFTER_NAME
) : KotlinEditorAutoTestCase(), FileReaderHolder by FileReaderHolder() {
    protected lateinit var testEditor: TextEditorTest
        private set

    protected abstract fun performTest(fileText: String, expectedFileText: String)

    protected open val loadFilesBeforeOpeningEditor = false

    override fun doSingleFileAutoTest(testPath: String) {
        val fileText = loadEditor(testPath)

        val afterTestPath: String =
            if (afterPosition == AfterSuffixPosition.AFTER_NAME) testPath + AFTER_FILE_EXTENSION
            else testPath.substring(
                0,
                testPath.length - extension.length
            ) + AFTER_FILE_EXTENSION + extension

        performTest(fileText, fileReader(afterTestPath))
    }

    override fun doMultiFileAutoTest(testFolder: File) {
        val (files, target) = getTestFiles(testFolder, fileReader)

        if (loadFilesBeforeOpeningEditor) {
            loadFiles(files)
            testEditor = configureEditor(target.fileName, target.content, target.packageName)
        } else {
            testEditor = configureEditor(target.fileName, target.content, target.packageName)
            loadFiles(files)
        }

        performTest(target.content, target.contentAfter)
    }

    override fun doAutoTestWithDependencyFile(mainTestPath: String, dependencyFile: File) {
        val fileText: String = if (loadFilesBeforeOpeningEditor) {
            loadDependencyFile(dependencyFile)
            loadEditor(mainTestPath)
        } else loadEditor(mainTestPath).also {
            loadDependencyFile(dependencyFile)
        }

        performTest(fileText, fileReader(mainTestPath + AFTER_FILE_EXTENSION))
    }

    private fun loadFiles(files: Collection<SourceFileData>) = files.forEach {
        createSourceFile(it.packageName, it.fileName, it.content)
    }

    private fun loadEditor(mainTestPath: String): String {
        val fileText = fileReader(mainTestPath)
        testEditor = configureEditor(
            KotlinTestUtils.getNameByPath(mainTestPath),
            fileText,
            SourceFileData.getPackageFromContent(fileText)
        )
        return fileText
    }

    private fun loadDependencyFile(dependencyFile: File) {
        try {
            val dependencySourceFile = SourceFileData(dependencyFile.name, fileReader(dependencyFile.absolutePath))
            val fileName = dependencySourceFile.fileName
            val dependencyFileName =
                fileName.substring(0, fileName.indexOf(KotlinEditorAutoTestCase.FILE_DEPENDENCY_SUFFIX)) +
                        "_dependency" + extension
            createSourceFile(
                dependencySourceFile.packageName, dependencyFileName,
                dependencySourceFile.content
            )
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

    }
}

private class WithAfterSourceFileData(file: File, content: String, val contentAfter: String) :
    SourceFileData(file.name, content)

private fun getTestFiles(
    testFolder: File,
    fileReader: (String) -> String
): Pair<Collection<SourceFileData>, WithAfterSourceFileData> {
    val (afterFiles, files) = testFolder.listFiles()
        ?.partition { it.name.endsWith(KotlinEditorAutoTestCase.AFTER_FILE_EXTENSION) }
        ?: throw AssertionError("$testFolder is not a directory")

    val afterFile = afterFiles.singleOrNull()
        ?: throw AssertionError("No target file or multiple target files found")

    val targetName = afterFile.name.replace(KotlinEditorAutoTestCase.AFTER_FILE_EXTENSION, "")
    val (targetFiles, otherFiles) = files.partition { it.name == targetName }
    val targetFile = targetFiles.singleOrNull()
        ?: throw AssertionError("No target file found for \'$\' file")

    return Pair(
        otherFiles.map { SourceFileData(it.name, fileReader(it.absolutePath)) },
        WithAfterSourceFileData(targetFile, fileReader(targetFile.absolutePath), fileReader(afterFile.absolutePath))
    )
}

enum class AfterSuffixPosition {
    BEFORE_DOT, AFTER_NAME
}

