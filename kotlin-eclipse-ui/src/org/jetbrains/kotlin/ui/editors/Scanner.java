package org.jetbrains.kotlin.ui.editors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.text.rules.WordRule;

public class Scanner extends RuleBasedScanner {

    private static final String[] kotlinKeywords = { 
        "package", 
        "as", 
        "type", 
        "class", 
        "this", 
        "super", 
        "val", 
        "var",
        "fun", 
        "for", 
        "null", 
        "true", 
        "false", 
        "is", 
        "in", 
        "throw", 
        "return", 
        "break", 
        "continue", 
        "object", 
        "if",
        "try", 
        "else", 
        "while", 
        "do", 
        "when", 
        "trait", 
        "This" };

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
    
    public static final String[] getAllKeywords() {
        return kotlinKeywords;
    }
}
