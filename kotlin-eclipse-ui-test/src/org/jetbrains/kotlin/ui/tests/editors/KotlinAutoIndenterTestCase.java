package org.jetbrains.kotlin.ui.tests.editors;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.jetbrains.kotlin.testframework.editor.TextEditorTest;
import org.jetbrains.kotlin.testframework.utils.WorkspaceUtil;
import org.junit.After;
import org.junit.Before;

public class KotlinAutoIndenterTestCase {
	
	private TextEditorTest testEditor;
	
	@After
	public void deleteEditingFile() {
		if (testEditor != null) {
			testEditor.deleteEditingFile();
		}
		
		boolean spacesForTabsBeforeConfigure = getStore().getBoolean(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS);
		int tabWidthBeforeConfigure = getStore().getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH);
		
		setStorePreference(spacesForTabsBeforeConfigure, tabWidthBeforeConfigure);
	}
	
	@Before
	public void refreshWorkspace() {
		WorkspaceUtil.refreshWorkspace();
		try {
			Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_REFRESH, new NullProgressMonitor());
		} catch (OperationCanceledException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	
    protected void doTest(String input, String expected) {
    	testEditor = configureEditor(input);
    	if (input.contains(TextEditorTest.CARET)) {
    		testEditor.typeEnter();
    	}
    	
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
