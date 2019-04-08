package org.jetbrains.kotlin.ui.tests.editors.quickfix.autoimport

import org.jetbrains.kotlin.testframework.editor.KotlinEditorWithAfterFileTestCase
import org.junit.Before
import org.jetbrains.kotlin.ui.editors.quickfix.KotlinMarkerResolutionGenerator
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.junit.Assert
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils
import org.jetbrains.kotlin.ui.editors.quickfix.KotlinMarkerResolution
import org.jetbrains.kotlin.testframework.editor.TextEditorTest
import org.jetbrains.kotlin.ui.editors.findDiagnosticsBy
import org.eclipse.jface.text.source.TextInvocationContext

private val expectedQuickFixRegex = "\"(.*?)\"".toRegex()

abstract class KotlinQuickFixTestCase : KotlinEditorWithAfterFileTestCase() {
    
    @Before
    fun before() {
        configureProject();
    }
    
    override fun performTest(fileText: String, expectedFileText: String) {
        val firstLine = fileText.split("\n")[0]
        val splattedLine = expectedQuickFixRegex.find(firstLine)
        val expectedLabel = splattedLine!!.groups[1]!!.value
        
        val foundProposals = getProposals(testEditor)
        val resolution = foundProposals.find { it.label == expectedLabel }
        
        Assert.assertNotNull(
                "Expected proposal with label \"$expectedLabel\" wasn't found. Found proposals:\n${foundProposals.joinToString("\n") { it.label }}",
                resolution)
        
        resolution!!.apply(testEditor.editingFile)
        
        EditorTestUtils.assertByEditor(testEditor.editor, expectedFileText);
    }
}

fun getProposals(testEditor: TextEditorTest): List<KotlinMarkerResolution> {
    val editor = testEditor.editor as KotlinFileEditor
    val diagnostics = findDiagnosticsBy(TextInvocationContext(editor.viewer, testEditor.caretOffset, 0), editor)
    
    return KotlinMarkerResolutionGenerator.getResolutions(diagnostics)
}