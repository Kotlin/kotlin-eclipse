package org.jetbrains.kotlin.ui.tests.editors;

import junit.framework.Assert;

import org.jetbrains.kotlin.ui.editors.ColorManager;
import org.jetbrains.kotlin.ui.editors.PartitionScanner;
import org.jetbrains.kotlin.ui.editors.Scanner;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;

public class KotlinHighlightningScannerTestCase {

	protected void doTest(String input, IToken expectedToken, boolean withPartitionScanner) {
		RuleBasedScanner scanner;
		if (withPartitionScanner) {
			scanner = new PartitionScanner();
		} else {
			scanner = new Scanner(new ColorManager());
		}
		
		scanner.setRange(new Document(input), 0, input.length());
		
		IToken actualToken = scanner.nextToken();
		
		Assert.assertEquals(actualToken.getData(), expectedToken.getData());
	}
}
