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
package org.jetbrains.kotlin.ui.tests.editors.navigation

import org.jetbrains.kotlin.testframework.editor.KotlinProjectTestCase
import org.junit.Before
import java.io.File
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.core.tests.diagnostics.JetTestUtils
import org.jetbrains.kotlin.core.tests.diagnostics.JetTestUtils.TestFileFactoryNoModules
import org.junit.Assert
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.jetbrains.kotlin.ui.editors.KotlinOpenSuperImplementationAction
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils

abstract class KotlinNavigationToSuperTestCase : KotlinProjectTestCase() {
    @Before
    fun confnigure() {
        configureProject()
    }
    
    fun doTest(filePath: String) {
        val content = FileUtil.loadFile(File(filePath))
        val (before, after) = loadBeforeAndAfterParts(content)
        
        val testEditor = configureEditor("before.kt", before)
        
        testEditor.editor.getAction(KotlinOpenSuperImplementationAction.ACTION_ID).run()
        
        EditorTestUtils.assertByEditor(testEditor.editor, after);
    }
    
    private fun loadBeforeAndAfterParts(content: String): NavigationTestData {
        val files = JetTestUtils.createTestFiles("", content, object : TestFileFactoryNoModules<String>() {
            override fun create(fileName: String, text: String, directives: MutableMap<String, String>): String {
                val firstLineEnd = text.indexOf('\n')
                return text.substring(firstLineEnd + 1).trim()
            }
        })
        
        Assert.assertTrue("Exactly two files expected: ", files.size == 2)
        
        return NavigationTestData(files[0], files[1])
    }
    
    private data class NavigationTestData(val before: String, val after: String)
}