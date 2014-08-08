/*******************************************************************************
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *******************************************************************************/
package org.jetbrains.kotlin.ui.tests.editors;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.jetbrains.kotlin.testframework.editor.KotlinProjectTestCase;
import org.jetbrains.kotlin.testframework.editor.TextEditorTest;
import org.jetbrains.kotlin.testframework.editor.KotlinEditorTestCase.Separator;
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils;
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils;
import org.junit.After;
import org.junit.Before;

public abstract class KotlinAutoIndenterTestCase extends KotlinProjectTestCase {
    private int initialSpacesCount;
	private Separator initialSeparator;

	@Before
	public void configure() {
    	configureProject();
    	
    	initialSeparator = EditorsUI.getPreferenceStore().getBoolean(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS) ? Separator.TAB : Separator.SPACE;
		initialSpacesCount = EditorsUI.getPreferenceStore().getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH);
	}
    
	@Override
	@After
	public void afterTest() {
		super.afterTest();
		
		EditorsUI.getPreferenceStore().setValue(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS, (Separator.SPACE == initialSeparator));
		EditorsUI.getPreferenceStore().setValue(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH, initialSpacesCount);
	}

	protected void doTest(String input, String expected) {
		String resolvedInput = KotlinTestUtils.resolveTestTags(input);
    	TextEditorTest testEditor = configureEditor("Test.kt", resolvedInput);
    	setStorePreference(false, 2);
    	
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
}
