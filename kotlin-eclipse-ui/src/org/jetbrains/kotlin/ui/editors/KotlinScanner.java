package org.jetbrains.kotlin.ui.editors;

import java.util.*;

import org.eclipse.jface.text.rules.*;
import org.eclipse.jface.text.*;

public class KotlinScanner extends RuleBasedScanner {
    // TODO: probably we need to load it from configuration file
    private static String[] kotlinKeywords = {
        "package", 
        "import", 
        "type", 
        "class", 
        "trait", 
        "by",
        "where",
        "fun",
        "if",
        "else",
        "try",
        "catch",
        "finally",
        "for",
        "while",
        "do",
        "super",
        "null",
        "true",
        "false",
        "throw",
        "return",
        "break",
        "continue"};

	public KotlinScanner(KotlinColorManager manager) {
        IToken keyword = 
                new Token(
                        new TextAttribute(manager.getColor(IKotlinColorConstants.KEYWORD)));
        IToken string =
                new Token(
                        new TextAttribute(manager.getColor(IKotlinColorConstants.STRING)));

        List<IRule> rulesList = new ArrayList<IRule>();
        
        WordRule wr = new WordRule(new KotlinWordDetector());
        for (String word : kotlinKeywords) {
            wr.addWord(word, keyword);
        }
        rulesList.add(wr);
        rulesList.add(new SingleLineRule("\"", "\"", string, '\\'));
        rulesList.add(new WhitespaceRule(new KotlinWhitespaceDetector()));
		
        IRule[] rules = new IRule[rulesList.size()];
		rules = rulesList.toArray(rules);

		setRules(rules);
	}

	private class KotlinWordDetector implements IWordDetector {
        @Override
        public boolean isWordStart(char c) {
            return Character.isJavaIdentifierStart(c);
        }

        @Override
        public boolean isWordPart(char c) {
            return Character.isJavaIdentifierPart(c);
        }
    }

}
