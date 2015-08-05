package org.jetbrains.kotlin.ui.tests.editors.quickfix.intentions

import org.jetbrains.kotlin.testframework.utils.EditorTestUtils
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor
import org.jetbrains.kotlin.testframework.editor.KotlinProjectTestCase
import org.junit.Before
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils
import org.jetbrains.kotlin.ui.overrideImplement.KotlinOverrideMembersAction
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.eclipse.jface.text.TextSelection
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants
import org.eclipse.ui.editors.text.EditorsUI

open class KotlinOverrideMembersTestCase : KotlinProjectTestCase() {
    @Before
    fun configure() {
        configureProject()
    }
    
    fun doTest(testPath: String) {
        val fileText = KotlinTestUtils.getText(testPath)
        val testEditor = configureEditor(KotlinTestUtils.getNameByPath(testPath), fileText)
        
        val action = KotlinOverrideMembersAction(testEditor.getEditor() as KotlinFileEditor, true)
        
        action.run(TextSelection(testEditor.getDocument(), testEditor.getCaretOffset(), 0))
        
        val expected = KotlinTestUtils.getText("${testPath}.after")
        EditorTestUtils.assertByEditor(testEditor.getEditor(), 
                KotlinImplementMethodsTestCase.removeCaretAndSelection(expected))
    }
    
    private fun assertByEditor() {
        
    }
}