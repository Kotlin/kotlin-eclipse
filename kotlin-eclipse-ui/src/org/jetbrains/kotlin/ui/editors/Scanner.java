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

    public Scanner(ColorManager manager) {
        IToken keyword = new Token(new TextAttribute(manager.getColor(IColorConstants.KEYWORD)));
        IToken string = new Token(new TextAttribute(manager.getColor(IColorConstants.STRING)));

        List<IRule> rulesList = new ArrayList<IRule>();

        IToken defaultToken = new Token(new TextAttribute(manager.getColor(IColorConstants.DEFAULT)));
        WordRule wr = new WordRule(new WordDetector(), defaultToken);
        for (String word : KeywordManager.getAllKeywords()) {
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
