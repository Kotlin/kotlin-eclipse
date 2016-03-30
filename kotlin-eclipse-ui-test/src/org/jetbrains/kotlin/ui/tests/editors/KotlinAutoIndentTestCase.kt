package org.jetbrains.kotlin.ui.tests.editors

import org.jetbrains.kotlin.testframework.editor.KotlinEditorWithAfterFileTestCase
import org.junit.Before
import org.eclipse.jface.text.TextUtilities
import org.eclipse.ui.editors.text.EditorsUI
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils
import org.jetbrains.kotlin.ui.tests.editors.formatter.KotlinFormatActionTestCase
import org.jetbrains.kotlin.ui.formatter.settings
import com.intellij.psi.codeStyle.CodeStyleSettings
import org.junit.After

abstract class KotlinAutoIndentTestCase : KotlinEditorWithAfterFileTestCase() {
    @Before
    fun before() {
        configureProject()
    }
    
    @After
    fun setDefaultSettings() {
        settings = CodeStyleSettings()
    }
    
    override fun performTest(fileText: String, expectedFileText: String) {
        KotlinFormatActionTestCase.configureSettings(fileText)
        
        EditorsUI.getPreferenceStore().setValue(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS, true)
        EditorsUI.getPreferenceStore().setValue(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH, 4)
        
        testEditor.typeEnter()
        
        EditorTestUtils.assertByEditor(testEditor.getEditor(), expectedFileText)
    }

}