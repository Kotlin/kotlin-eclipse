package org.jetbrains.kotlin.ui.editors;

import org.eclipse.jdt.ui.text.IColorManager;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;

public class Scanner extends RuleBasedScanner {
    
    private static final IColorManager manager = new ColorManager(); 
    public static final IToken STRING = new Token(new TextAttribute(manager.getColor(IColorConstants.STRING)));

    public Scanner(ColorManager manager) {
        setRules(new IRule[] { new SingleLineRule("\"", "\"", STRING, '\\') });
    }
}
