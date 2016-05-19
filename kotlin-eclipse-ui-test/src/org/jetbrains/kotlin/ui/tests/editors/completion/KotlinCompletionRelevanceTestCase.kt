/*******************************************************************************
 * Copyright 2000-2016 JetBrains s.r.o.
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

import org.eclipse.jface.text.contentassist.ICompletionProposal
import org.jetbrains.kotlin.testframework.editor.KotlinProjectTestCase
import org.jetbrains.kotlin.testframework.utils.InTextDirectivesUtils
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.junit.Assert
import org.junit.Before

abstract class KotlinCompletionRelevanceTestCase : KotlinProjectTestCase() {
    @Before
    fun before() {
        configureProject()
    }
    
    protected fun doTest(testPath: String) {
        val fileText = KotlinTestUtils.getText(testPath)
        
        val testEditor = configureEditor(KotlinTestUtils.getNameByPath(testPath), fileText)
        
        KotlinTestUtils.joinBuildThread()
        
        val actualProposals = getCompletionProposals(testEditor.editor as KotlinFileEditor)
        
        val expectedItemsOrder = InTextDirectivesUtils.findListWithPrefixes(fileText, "// ORDER:")
        Assert.assertTrue("There should be 'ORDER' items", expectedItemsOrder.isNotEmpty())
        
        checkPreferredCompletionItems(actualProposals, expectedItemsOrder)
    }
    
    private fun checkPreferredCompletionItems(actualProposals: Array<ICompletionProposal>, expectedOrder: List<String>) {
        var expectedItemIndex = 0
        for (proposal in actualProposals) {
            val actualItem = proposal.stringToInsert()
            if (actualItem == expectedOrder[expectedItemIndex]) {
                expectedItemIndex++
            }
            
            if (expectedItemIndex == expectedOrder.size) {
                break
            }
        }
        
        if (expectedItemIndex != expectedOrder.size) {
            Assert.fail("Order is not equal: actual - ${actualProposals.map { it.stringToInsert() }} and expected - ${expectedOrder}")
        }
    }
}