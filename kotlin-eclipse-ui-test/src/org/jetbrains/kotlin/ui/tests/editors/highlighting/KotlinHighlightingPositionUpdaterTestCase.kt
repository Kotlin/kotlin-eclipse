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

abstract class KotlinHighlightingPositionUpdaterTestCase : KotlinEditorWithAfterFileTestCase() {
    val testCategory = "testPositionUpdater"
    val positionUpdater = KotlinPositionUpdater(testCategory)
    val posTag = "<pos>"
    
    @Before
    fun before() {
        configureProject();
    }
    
    @After
    fun after() {
        testEditor.document.removePositionUpdater(positionUpdater)
        testEditor.document.removePositionCategory(testCategory)
    }
    
    override fun performTest(fileText: String, expected: String) {
        val document = testEditor.document
        configureDocument(document)
        addHighlihgintPositions()
        
        val typedText = TypingUtils.typedText(fileText)
        document.replace(testEditor.caretOffset, 0, typedText)
        
        val documentText = StringBuilder(document.get())
        var shift = 0
        for (position in document.getPositions(testCategory)) {
            documentText.insert(position.getOffset() + shift, posTag)
            shift += posTag.length
            
            documentText.insert(position.getOffset() + position.getLength() + shift, posTag)
            shift += posTag.length
        }
        
        Assert.assertEquals(expected, documentText)
    }
    
    private fun addHighlihgintPositions() {
        val ktFile = (testEditor.editor as KotlinEditor).parsedFile!!
        val document = testEditor.document
        val highlightingVisitor = KotlinSemanticHighlightingVisitor(ktFile, document, testEditor.testJavaProject.javaProject)
        highlightingVisitor.computeHighlightingRanges().forEach { 
            if (it is StyleAttributes) document.addPosition(testCategory, it)
        }
    }
    
    private fun configureDocument(document: IDocument) {
        document.addPositionCategory(testCategory)
        document.addPositionUpdater(positionUpdater)
    }
}