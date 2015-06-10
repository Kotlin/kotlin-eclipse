package org.jetbrains.kotlin.ui.tests.editors;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.jetbrains.kotlin.testframework.editor.KotlinProjectTestCase;
import org.jetbrains.kotlin.testframework.editor.TextEditorTest;
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils;
import org.junit.Assert;
import org.junit.Before;

public class KotlinFormatCorrectSeparatorsTestCase extends KotlinProjectTestCase {
	@Override
	@Before
    public void beforeTest() {
		configureProject();
		super.beforeTest();
    }
	
	protected void doTest(String testPath) throws BadLocationException {
		String input = KotlinTestUtils.getText(testPath);
		TextEditorTest testEditor = configureEditor("Test.kt", input);
		String expectedLineDelimiter = TextUtilities.getDefaultLineDelimiter(testEditor.getDocument());
		
		testEditor.runFormatAction();
		
		Set<String> lineDelimiters = getLineDelimiters(testEditor.getDocument());
		
		Assert.assertEquals(1, lineDelimiters.size());
		Assert.assertEquals(expectedLineDelimiter, lineDelimiters.iterator().next());
	}
	
	private Set<String> getLineDelimiters(IDocument document) throws BadLocationException {
		Set<String> lineDelimiters = new HashSet<String>();
		for (int i = 0; i < document.getNumberOfLines() - 1; ++i) {
			lineDelimiters.add(document.getLineDelimiter(i));
		}
		
		return lineDelimiters;
	}
}
