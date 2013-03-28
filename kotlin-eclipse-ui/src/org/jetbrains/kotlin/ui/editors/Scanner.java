package org.jetbrains.kotlin.ui.editors;

import java.util.*;

import org.eclipse.jface.text.rules.*;
import org.eclipse.jface.text.*;

public class Scanner extends RuleBasedScanner {
    // TODO: probably we need to load it from configuration file
    private static final String[] kotlinKeywords = { "package", "import", "type", "class", "trait", "by", "where",
            "fun", "if", "else", "try", "catch", "finally", "for", "while", "do", "super", "null", "true", "false",
            "throw", "return", "break", "continue" };

    public Scanner(ColorManager manager) {
        IToken keyword = new Token(new TextAttribute(manager.getColor(IColorConstants.KEYWORD)));
        IToken string = new Token(new TextAttribute(manager.getColor(IColorConstants.STRING)));

        List<IRule> rulesList = new ArrayList<IRule>();

        WordRule wr = new WordRule(new WordDetector());
        for (String word : kotlinKeywords) {
            wr.addWord(word, keyword);
        }
        rulesList.add(wr);
        rulesList.add(new SingleLineRule("\"", "\"", string, '\\'));
        rulesList.add(new WhitespaceRule(new WhitespaceDetector()));

        IRule[] rules = new IRule[rulesList.size()];
        rules = rulesList.toArray(rules);

        setRules(rules);
    }

}
