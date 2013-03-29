package org.jetbrains.kotlin.ui.editors;

import org.eclipse.jface.text.rules.IWordDetector;

public class WordDetector implements IWordDetector {
    @Override
    public boolean isWordStart(char c) {
        return Character.isJavaIdentifierStart(c);
    }

    @Override
    public boolean isWordPart(char c) {
        return Character.isJavaIdentifierPart(c);
    }
}
