package org.jetbrains.kotlin.ui.tests.editors;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.jetbrains.kotlin.testframework.editor.TextEditorTest;

public class KotlinAutoIndenterTestCase {

    protected void doTest(String input, String expected) {
    	boolean spacesForTabsBeforeConfigure = getStore().getBoolean(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS);
		int tabWidthBeforeConfigure = getStore().getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH);
		
    	TextEditorTest testEditor = configureEditor(input);
    	if (input.contains(TextEditorTest.CARET)) {
    		testEditor.typeEnter();
    	}
    	
    	setStorePreference(spacesForTabsBeforeConfigure, tabWidthBeforeConfigure);
    	
    	testEditor.assertByEditor(expected);
    }
	
	protected IPreferenceStore getStore() {
    	return EditorsUI.getPreferenceStore();
    }
    
    protected void setStorePreference(boolean isSpacesForTabs, int tabWidth) {
    	getStore().setValue(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS, isSpacesForTabs);
		getStore().setValue(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH, tabWidth);
    }
    
    protected TextEditorTest configureEditor(String content) {
    	TextEditorTest testEditor = new TextEditorTest();
		testEditor.createEditor("Test.kt", content);
		
		setStorePreference(false, 2);
		
		return testEditor;
    }
}
