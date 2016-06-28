package org.jetbrains.kotlin.ui.tests.editors.completion

import org.jetbrains.kotlin.testframework.editor.KotlinProjectTestCase
import org.junit.Before
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.jetbrains.kotlin.ui.editors.codeassist.KotlinCompletionProcessor
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.testframework.editor.TextEditorTest
import org.junit.Assert

open public class KotlinFunctionParameterInfoTestCase : KotlinProjectTestCase() {
    @Before
    fun before() {
        configureProject()
    }
    
    protected fun doTest(testPath: String) {
        val fileText = KotlinTestUtils.getText(testPath)
        val testEditor = configureEditor(KotlinTestUtils.getNameByPath(testPath), fileText)
        
        val actualResult = getContextInformation(testEditor.getEditor() as KotlinFileEditor)
        val expectedResult = getExpectedResult(testEditor)
        
        val agreement = actualResult.size == expectedResult.size && 
                actualResult.all { actualResult ->
                    expectedResult.any { expectedResult ->
                        actualResult == expectedResult
                    }
                }
        
        Assert.assertTrue("$expectedResult are not equals to $actualResult", agreement)
    }
    
    private fun getExpectedResult(testEditor: TextEditorTest): List<String> {
        val jetFile = KotlinPsiManager.getParsedFile(testEditor.getEditingFile())
        val lastChild = jetFile.getLastChild()
        val expectedText = if (lastChild.getNode().getElementType() == KtTokens.BLOCK_COMMENT) {
            val lastChildText = lastChild.getText()
            lastChildText.substring(2, lastChildText.length - 2).trim()
        } else { // EOL_COMMENT
            lastChild.getText().substring(2).trim()
        }
        
        val regex = "\\((.*)\\)".toRegex()
        val beginHighlightRegex = "<highlight>".toRegex()
        val endHighlightRegex = "</highlight>".toRegex()
        val noParametersRegex = "<no parameters>".toRegex()
        return expectedText.split("\n")
            .map { line ->
                val match = regex.find(line) 
                val displayString = match!!.groups[1]!!.value
                displayString
                    .replace(beginHighlightRegex, "")
                    .replace(endHighlightRegex, "")
                    .replace(noParametersRegex, "")
            }
            .filter { it.isNotBlank() }
    }
    
    private fun getContextInformation(editor: KotlinFileEditor): List<String> {
        val completionProcessor = KotlinCompletionProcessor(editor)
        val contextInformation = completionProcessor.computeContextInformation(editor.getViewer(), 
                KotlinTestUtils.getCaret(editor))
        return contextInformation.map { it.getInformationDisplayString() }
    }
}