package org.jetbrains.kotlin.ui.tests.editors;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.jetbrains.kotlin.testframework.editor.KotlinEditorTestCase;
import org.jetbrains.kotlin.testframework.editor.TextEditorTest;
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils;

public class KotlinAutoIndenterTestCase extends KotlinEditorTestCase {
	
    protected void doTest(String input, String expected) {
    	testEditor = configureEditor(input);
    	if (input.contains(TextEditorTest.CARET)) {
    		testEditor.typeEnter();
    	}
    	
    	EditorTestUtils.assertByEditor(testEditor.getEditor(), expected);
    }
	
	protected IPreferenceStore getStore() {
    	return EditorsUI.getPreferenceStore();
    }
    
    protected void setStorePreference(boolean isSpacesForTabs, int tabWidth) {
    	getStore().setValue(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS, isSpacesForTabs);
		getStore().setValue(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH, tabWidth);
    }
    
    protected TextEditorTest configureEditor(String content) {
    	testEditor = super.configureEditor("Test.kt", content);
		
		setStorePreference(false, 2);
		
		return testEditor;
    }
}
