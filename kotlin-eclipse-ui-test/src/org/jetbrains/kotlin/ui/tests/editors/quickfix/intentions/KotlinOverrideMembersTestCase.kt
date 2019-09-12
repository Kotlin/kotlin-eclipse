package org.jetbrains.kotlin.ui.tests.editors.quickfix.intentions

import org.eclipse.jface.text.TextSelection
import org.jetbrains.kotlin.testframework.editor.KotlinProjectTestCase
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils
import org.jetbrains.kotlin.testframework.utils.FileReaderHolder
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.jetbrains.kotlin.ui.overrideImplement.KotlinOverrideMembersAction
import org.junit.Before

open class KotlinOverrideMembersTestCase : KotlinProjectTestCase(), FileReaderHolder by FileReaderHolder() {
    @Before
    fun configure() {
        configureProject()
    }
    
    fun doTest(testPath: String) {
        val fileText = fileReader(testPath)
        val testEditor = configureEditor(KotlinTestUtils.getNameByPath(testPath), fileText)
        
        val action = KotlinOverrideMembersAction(testEditor.getEditor() as KotlinFileEditor, true)
        
        action.run(TextSelection(testEditor.getDocument(), testEditor.getCaretOffset(), 0))
        
        val expected = fileReader("${testPath}.after")
        EditorTestUtils.assertByEditor(testEditor.getEditor(), 
                KotlinImplementMethodsTestCase.removeCaretAndSelection(expected))
    }
    
    private fun assertByEditor() {
        
    }
}