package org.jetbrains.kotlin.ui.tests.refactoring.extract

import org.eclipse.core.runtime.NullProgressMonitor
import org.eclipse.jface.text.ITextSelection
import org.jetbrains.kotlin.testframework.editor.KotlinProjectTestCase
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils
import org.jetbrains.kotlin.testframework.utils.FileReaderHolder
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.jetbrains.kotlin.ui.refactorings.extract.KotlinExtractVariableRefactoring
import org.junit.Before

public abstract class KotlinExtractVariableTestCase : KotlinProjectTestCase(), FileReaderHolder by FileReaderHolder() {
    @Before
    fun before() {
        configureProject()
    }
    
    @JvmOverloads
    fun doTest(testPath: String, newName: String = "i") {
        val fileText = fileReader(testPath)
        val testEditor = configureEditor(KotlinTestUtils.getNameByPath(testPath), fileText)
        val editor = testEditor.getEditor() as KotlinFileEditor
        
        val refactoring = KotlinExtractVariableRefactoring(editor.getSelectionProvider().getSelection() as ITextSelection, editor)
        val monitor = NullProgressMonitor()
        
        refactoring.checkInitialConditions(monitor)
        refactoring.newName = newName
        refactoring.createChange(monitor).perform(monitor)
        
        EditorTestUtils.assertByEditor(editor, fileReader("${testPath}.after"))
    }
}