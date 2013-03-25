package org.jetbrains.kotlin.ui.editors;

import org.eclipse.jface.text.rules.*;

public class KotlinPartitionScanner extends RuleBasedPartitionScanner {
	public final static String KOTLIN_COMMENT = "__kotlin_comment";

	public KotlinPartitionScanner() {

		IToken comment = new Token(KOTLIN_COMMENT);

		IPredicateRule[] rules = new IPredicateRule[2];

		rules[0] = new MultiLineRule("/*", "*/", comment);
		rules[1] = new EndOfLineRule("//", comment);

		setPredicateRules(rules);
	}
}
