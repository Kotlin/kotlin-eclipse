package org.jetbrains.kotlin.ui.editors;

import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.Token;

public class PartitionScanner extends RuleBasedPartitionScanner {
    public final static String KOTLIN_COMMENT = "__kotlin_comment";

    public PartitionScanner() {

        IToken comment = new Token(KOTLIN_COMMENT);

        IPredicateRule[] rules = new IPredicateRule[2];

        rules[0] = new MultiLineRule("/*", "*/", comment, (char)0, true);
        rules[1] = new EndOfLineRule("//", comment);

        setPredicateRules(rules);
    }
}
