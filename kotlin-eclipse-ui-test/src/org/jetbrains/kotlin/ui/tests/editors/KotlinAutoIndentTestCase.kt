package org.jetbrains.kotlin.ui.tests.editors

import org.eclipse.ui.editors.text.EditorsUI
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants
import org.jetbrains.kotlin.testframework.editor.AfterSuffixPosition
import org.jetbrains.kotlin.testframework.editor.KotlinEditorWithAfterFileTestCase
import org.jetbrains.kotlin.testframework.utils.CodeStyleConfigurator
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils
import org.junit.After
import org.junit.Before

abstract class KotlinAutoIndentTestCase(
    afterSuffixPosition: AfterSuffixPosition = AfterSuffixPosition.AFTER_NAME
) : KotlinEditorWithAfterFileTestCase(afterSuffixPosition) {
    @Before
    fun before() {
        configureProject()
    }
    
    @After
    fun setDefaultSettings() {
        CodeStyleConfigurator.deconfigure(testProject.project)
    }
    
    override fun performTest(fileText: String, expectedFileText: String) {
        CodeStyleConfigurator.configure(testProject.project, fileText)
        
        EditorsUI.getPreferenceStore().setValue(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS, true)
        EditorsUI.getPreferenceStore().setValue(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH, 4)
        
        testEditor.typeEnter()
        
        EditorTestUtils.assertByEditor(testEditor.getEditor(), expectedFileText)
    }

}