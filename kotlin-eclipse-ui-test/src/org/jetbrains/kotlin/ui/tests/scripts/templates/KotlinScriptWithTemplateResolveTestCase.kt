package org.jetbrains.kotlin.ui.tests.scripts.templates

import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.testframework.editor.KotlinProjectTestCase
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils
import org.jetbrains.kotlin.ui.editors.KotlinEditor
import org.junit.Assert
import org.junit.Before

abstract class KotlinScriptWithTemplateResolveTestCase : KotlinProjectTestCase() {
    @Before
    fun configure() {
        configureProject()
    }
    
    protected fun doTest(testPath: String) {
        val fileText = KotlinTestUtils.getText(testPath)
        
        val testEditor = configureEditor(KotlinTestUtils.getNameByPath(testPath), fileText)
        val ktEditor = testEditor.editor as KotlinEditor
        
        val analysisResult = KotlinAnalyzer.analyzeFile(ktEditor.parsedFile!!).analysisResult
        val errorMessages = renderErrors(analysisResult).joinToString("\n")
        
        Assert.assertFalse(errorMessages, hasErrors(analysisResult))
    }
    
    private fun hasErrors(analysisResult: AnalysisResult): Boolean {
        return getErrors(analysisResult).isNotEmpty()
    }
    
    private fun renderErrors(analysisResult: AnalysisResult): List<String> {
        return getErrors(analysisResult).map { DefaultErrorMessages.render(it) }
    }
    
    private fun getErrors(analysisResult: AnalysisResult): List<Diagnostic> {
        return analysisResult.bindingContext.diagnostics.filter { it.severity == Severity.ERROR }
    }
}