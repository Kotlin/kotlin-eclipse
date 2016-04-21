package org.jetbrains.kotlin.ui.tests.editors.quickfix.autoimport

import org.jetbrains.kotlin.testframework.editor.KotlinEditorWithAfterFileTestCase
import org.junit.Before
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils
import org.eclipse.ui.ide.IDE
import org.eclipse.core.resources.IMarker
import org.eclipse.core.resources.IResource
import org.eclipse.jface.text.source.ISourceViewer
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.ui.editors.annotations.DiagnosticAnnotation
import org.jetbrains.kotlin.ui.editors.annotations.endOffset
import org.eclipse.jface.text.source.IAnnotationModel
import org.jetbrains.kotlin.ui.editors.quickfix.KotlinMarkerResolutionGenerator
import org.jetbrains.kotlin.ui.editors.annotations.AnnotationManager
import org.jetbrains.kotlin.ui.editors.quickassist.getBindingContext
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.junit.Assert
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil

private val expectedQuickFixRegex = "\"(.*?)\"".toRegex()

abstract class KotlinQuickFixTestCase : KotlinEditorWithAfterFileTestCase() {
    
    @Before
    fun before() {
        configureProject();
    }
    
    override fun performTest(fileText: String, expectedFileText: String) {
        val caretOffset = LineEndUtil.convertCrToDocumentOffset(testEditor.getDocument(), testEditor.getCaretOffset())
        
        val diagnostics = getBindingContext(
                KotlinPsiManager.INSTANCE.getParsedFile(testEditor.getEditingFile()),
                testEditor.getTestJavaProject().getJavaProject())!!
                    .diagnostics
                    .toList().filter { 
                        val range = it.psiElement.textRange
                        range.startOffset <= caretOffset && caretOffset <= range.endOffset
                     }
        
        val firstLine = fileText.split("\n")[0]
        val splittedLine = expectedQuickFixRegex.find(firstLine)
        val expectedLabel = splittedLine!!.groups[1]!!.value
        
        val resolution = KotlinMarkerResolutionGenerator.getResolutions(diagnostics).find { it.label == expectedLabel }
        
        Assert.assertNotNull(resolution)
        
        resolution!!.apply(testEditor.getEditingFile())
        
        EditorTestUtils.assertByEditor(testEditor.getEditor(), expectedFileText);
    }
}