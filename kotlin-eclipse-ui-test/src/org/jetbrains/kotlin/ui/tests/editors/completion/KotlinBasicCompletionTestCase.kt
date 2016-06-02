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
package org.jetbrains.kotlin.ui.tests.editors.completion

import org.eclipse.jdt.internal.ui.JavaPlugin
import org.eclipse.jdt.ui.PreferenceConstants
import org.jetbrains.kotlin.testframework.editor.KotlinProjectTestCase
import org.jetbrains.kotlin.testframework.utils.ExpectedCompletionUtils
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.junit.Assert
import org.junit.Before

abstract class KotlinBasicCompletionTestCase : KotlinProjectTestCase() {
    @Before
    fun configure() {
        configureProject()
    }

    protected fun doTest(testPath: String) {
        val fileText = KotlinTestUtils.getText(testPath)
        
        val shouldHideNonVisible = ExpectedCompletionUtils.shouldHideNonVisibleMembers(fileText)
        JavaPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.CODEASSIST_SHOW_VISIBLE_PROPOSALS, shouldHideNonVisible)
        
        val testEditor = configureEditor(KotlinTestUtils.getNameByPath(testPath), fileText)
        
        val actualProposals = getActualProposals(testEditor.getEditor() as KotlinFileEditor)
        
        val expectedNumber = ExpectedCompletionUtils.numberOfItemsShouldPresent(fileText)
        if (expectedNumber != null) {
            Assert.assertEquals("Different count of expected and actual proposals", expectedNumber, actualProposals.size)
        }
        
        val proposalSet = actualProposals.toSet()
        
        val expectedProposals = ExpectedCompletionUtils.itemsShouldExist(fileText)
        assertExists(expectedProposals, proposalSet)
        
        val expectedJavaOnlyProposals = ExpectedCompletionUtils.itemsJavaOnlyShouldExists(fileText)
        assertExists(expectedJavaOnlyProposals, proposalSet)
        
        val unexpectedProposals = ExpectedCompletionUtils.itemsShouldAbsent(fileText)
        assertNotExists(unexpectedProposals, proposalSet)
    }

    private fun getActualProposals(javaEditor: KotlinFileEditor): List<String> {
        return getCompletionProposals(javaEditor).map { it.stringToInsert() }
    }

    private fun assertExists(itemsShouldExist: List<String>, actualItems: Set<String>) {
        val missing = itemsShouldExist.filter { !actualItems.contains(it) }.toSet()
        
        if (missing.isNotEmpty()) {
            Assert.fail(getErrorMessage("Items not found.", itemsShouldExist, actualItems, missing, emptySet()))
        }
    }

    private fun assertNotExists(itemsShouldAbsent: List<String>, actualItems: Set<String>) {
        val added = itemsShouldAbsent.filter { actualItems.contains(it) }.toSet()
        
        if (added.isNotEmpty()) {
            Assert.fail(getErrorMessage("Items must be absent.", itemsShouldAbsent, actualItems, emptySet(), added))
        }
    }

    private fun getErrorMessage(message: String, expected: List<String>, actual: Set<String>, missing: Set<String>, added: Set<String>): String? {
        return StringBuilder().apply {
            append("$message\n")
            append("Expected: <\n")
            for (proposal in expected.sorted()) {
                if (missing.contains(proposal)) {
                    append("-")
                }
                append("$proposal\n")
            }
            append("> ")
            append("but was:<\n")
            for (proposal in actual.sorted()) {
                if (added.contains(proposal)) {
                    append("+")
                }
                append("proposal\n")
            }
            append(">")
        }.toString()
    }
}