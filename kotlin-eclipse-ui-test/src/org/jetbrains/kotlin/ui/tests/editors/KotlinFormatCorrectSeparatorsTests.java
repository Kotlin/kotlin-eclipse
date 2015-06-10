package org.jetbrains.kotlin.ui.tests.editors;

import org.eclipse.jface.text.BadLocationException;
import org.junit.Test;

public class KotlinFormatCorrectSeparatorsTests extends KotlinFormatCorrectSeparatorsTestCase {
	@Test
	public void testFileWithJavadoc() throws BadLocationException {
		doTest("testData/format/lineDelimiters/withJavaDoc.kt");
	}
	
	@Test
	public void testFileWithoutComments() throws BadLocationException {
		doTest("testData/format/lineDelimiters/withoutComments.kt");
	}
	
	@Test
	public void testFileWithLineComments() throws BadLocationException {
		doTest("testData/format/lineDelimiters/withLineComments.kt");
	}
	
	@Test
	public void testFileWithBlockComments() throws BadLocationException {
		doTest("testData/format/lineDelimiters/withBlockComments.kt");
	}
}
