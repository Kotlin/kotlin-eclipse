package org.jetbrains.kotlin.utils;

import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;

public class IndenterUtil {

    private static final char tabChar = '\t';
    private static final char spaceSeparator = ' ';
    
    public static String createWhiteSpace(int curIndent, int countBreakLines) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < countBreakLines; ++i) {
            stringBuilder.append(System.lineSeparator());
        }
        
        String tabAsSpaces = getTabAsSpaces();
        for (int i = 0; i < curIndent; ++i) {
            if (isSpacesForTabs()) {
                stringBuilder.append(tabAsSpaces);
            } else {
                stringBuilder.append(tabChar);
            }
        }

        return stringBuilder.toString();
    }
    
    private static String getTabAsSpaces() {
        StringBuilder res = new StringBuilder();
        if (isSpacesForTabs()) {
            for (int i = 0; i < getDefaultIndent(); ++i) {
               res.append(spaceSeparator);
            }
        }
        
        return res.toString();
    }
    
    public static int getDefaultIndent() {
        return EditorsUI.getPreferenceStore().getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH);
    }
    
    public static boolean isSpacesForTabs() {
        return EditorsUI.getPreferenceStore().getBoolean(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS);
    }
}
