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
package org.jetbrains.kotlin.ui.tests.editors.highlighting

import java.io.File
import org.jetbrains.kotlin.testframework.editor.KotlinEditorWithAfterFileTestCase
import org.junit.Before
import org.jetbrains.kotlin.testframework.utils.TypingUtils
import org.eclipse.jface.text.IDocument
import org.jetbrains.kotlin.ui.editors.highlighting.KotlinPositionUpdater
import org.junit.After
import org.junit.Assert
import org.jetbrains.kotlin.ui.editors.highlighting.KotlinSemanticHighlightingVisitor
import org.jetbrains.kotlin.ui.editors.KotlinEditor
import org.jetbrains.kotlin.ui.editors.highlighting.HighlightPosition.StyleAttributes
import org.eclipse.jface.text.IPositionUpdater

abstract class KotlinHighlightingPositionUpdaterTestCase : KotlinEditorWithAfterFileTestCase() {
    companion object {
        private val testCategory = "testPositionUpdater"
        private val posTag = "<pos>"
        private val posCloseTag = "</pos>"
    }
    
    @Before
    fun before() {
        configureProject();
    }
    
    override fun performTest(fileText: String, expected: String) {
        val positionUpdater = KotlinPositionUpdater(testCategory)
        try {
            val document = testEditor.document
                    
            configureDocument(document, positionUpdater)
            addHighlightingPositions()
            
            val typedText = TypingUtils.typedText(fileText)
            document.replace(testEditor.caretOffset, 0, typedText)
            
            val documentText = StringBuilder(document.get())
            var shift = 0
            for (position in document.getPositions(testCategory)) {
                if (position.isDeleted()) continue
                
                documentText.insert(position.getOffset() + shift, posTag)
                shift += posTag.length
                
                documentText.insert(position.getOffset() + position.getLength() + shift, posCloseTag)
                shift += posCloseTag.length
            }
            
            Assert.assertEquals(expected, documentText.toString())
        } finally {
            testEditor.document.removePositionUpdater(positionUpdater)
            testEditor.document.removePositionCategory(testCategory)
        }
    }
    
    private fun addHighlightingPositions() {
        val ktFile = (testEditor.editor as KotlinEditor).parsedFile!!
        val document = testEditor.document
        val highlightingVisitor = KotlinSemanticHighlightingVisitor(ktFile, document)
        highlightingVisitor.computeHighlightingRanges().forEach { 
            if (it is StyleAttributes) document.addPosition(testCategory, it)
        }
    }
    
    private fun configureDocument(document: IDocument, positionUpdater: IPositionUpdater) {
        document.addPositionCategory(testCategory)
        document.addPositionUpdater(positionUpdater)
    }
}