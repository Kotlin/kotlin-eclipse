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

import junit.framework.Assert;

import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.jetbrains.kotlin.testframework.editor.KotlinEditorTestCase;
import org.jetbrains.kotlin.ui.editors.ColorManager;
import org.jetbrains.kotlin.ui.editors.IColorConstants;

public class KotlinHighlightningTestCase extends KotlinEditorTestCase {
	
	private static final ColorManager COLOR_MANAGER = new ColorManager();
	private static final Color KEYWORD = COLOR_MANAGER.getColor(IColorConstants.KEYWORD);
	private static final Color STRING = COLOR_MANAGER.getColor(IColorConstants.STRING);
	private static final Color COMMENT = COLOR_MANAGER.getColor(IColorConstants.COMMENT);
	
	private static final String KEYWORD_OPEN = "<keyword>";
	private static final String KEYWORD_CLOSE = "</keyword>";
	private static final String STRING_OPEN = "<string>";
	private static final String STRING_CLOSE = "</string>";
	private static final String COMMENT_OPEN = "<comment>";
	private static final String COMMENT_CLOSE = "</comment>";
	

	protected void doTest(String input) {
		testEditor = configureEditor("Test.kt", removeColorTags(input));
		
		StyleRange[] styleRanges = testEditor.getEditor().getViewer().getTextWidget().getStyleRanges();
		String actualText = insertTokenTags(testEditor.getEditorInput(), styleRanges);
		
		Assert.assertEquals(resolveTestTags(input), actualText);
	}
	
	private String insertTokenTags(String text, StyleRange[] styleRanges) {
		int tokenShift = 0;
		StringBuilder input = new StringBuilder(text);
		for (StyleRange styleRange : styleRanges) {
			if (KEYWORD.equals(styleRange.foreground)) {
				tokenShift = insertTokenTag(input, KEYWORD_OPEN, styleRange.start, tokenShift);
				tokenShift = insertTokenTag(input, KEYWORD_CLOSE, styleRange.start + styleRange.length, tokenShift);
			}
			
			if (STRING.equals(styleRange.foreground)) {
				tokenShift = insertTokenTag(input, STRING_OPEN, styleRange.start, tokenShift);
				tokenShift = insertTokenTag(input, STRING_CLOSE, styleRange.start + styleRange.length, tokenShift);
			}
			
			if (COMMENT.equals(styleRange.foreground)) {
				tokenShift = insertTokenTag(input, COMMENT_OPEN, styleRange.start, tokenShift);
				tokenShift = insertTokenTag(input, COMMENT_CLOSE, styleRange.start + styleRange.length, tokenShift);

			}
		}
		
		return input.toString();
	}
	
	private int insertTokenTag(StringBuilder input, String insertText, int offset, int shift) {
		input.insert(offset + shift, insertText);
		return shift + insertText.length();
	}
	
	private String removeColorTags(String text) {
		return text
				.replaceAll(KEYWORD_OPEN, "")
				.replaceAll(KEYWORD_CLOSE, "")
				.replaceAll(STRING_OPEN, "")
				.replaceAll(STRING_CLOSE, "")
				.replaceAll(COMMENT_OPEN, "")
				.replaceAll(COMMENT_CLOSE, "");
				
	}
}
