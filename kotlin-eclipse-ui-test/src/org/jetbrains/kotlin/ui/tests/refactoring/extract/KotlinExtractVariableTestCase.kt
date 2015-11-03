package org.jetbrains.kotlin.ui.tests.refactoring.extract

import org.jetbrains.kotlin.testframework.editor.KotlinEditorWithAfterFileTestCase
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.jetbrains.kotlin.ui.refactorings.extract.KotlinExtractVariableAction
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils
import org.jetbrains.kotlin.ui.refactorings.extract.KotlinExtractVariableRefactoring
import org.eclipse.jface.text.ITextSelection
import org.eclipse.core.runtime.NullProgressMonitor
import org.junit.Before
import org.jetbrains.kotlin.testframework.editor.KotlinProjectTestCase
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils

public abstract class KotlinExtractVariableTestCase : KotlinProjectTestCase() {
    @Before
    fun before() {
        configureProject()
    }
    
    @JvmOverloads
    fun doTest(testPath: String, newName: String = "i") {
        val fileText = KotlinTestUtils.getText(testPath)
        val testEditor = configureEditor(KotlinTestUtils.getNameByPath(testPath), fileText)
        val editor = testEditor.getEditor() as KotlinFileEditor
        
        val refactoring = KotlinExtractVariableRefactoring(editor.getSelectionProvider().getSelection() as ITextSelection, editor)
        val monitor = NullProgressMonitor()
        
        refactoring.checkInitialConditions(monitor)
        refactoring.newName = newName
        refactoring.createChange(monitor).perform(monitor)
        
        EditorTestUtils.assertByEditor(editor, KotlinTestUtils.getText("${testPath}.after"))
    }
}