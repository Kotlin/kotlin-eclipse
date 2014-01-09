/*******************************************************************************
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *******************************************************************************/
package org.jetbrains.kotlin.utils;

import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.jetbrains.jet.lexer.JetTokens;

import com.intellij.psi.impl.source.tree.LeafPsiElement;

public class IndenterUtil {

    private static final char tabChar = '\t';
    private static final char spaceSeparator = ' ';
    private static final String LINE_SEPARATOR = "\n";
    
    public static String createWhiteSpace(int indent, int countBreakLines) {
        return createWhiteSpace(indent, countBreakLines, System.lineSeparator());
    }
    
    public static String createWhiteSpace(int curIndent, int countBreakLines, String lineSeparator) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < countBreakLines; ++i) {
            stringBuilder.append(lineSeparator);
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
    
    public static int getLineSeparatorsOccurences(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); ++i) {
            if (text.charAt(i) == LINE_SEPARATOR.charAt(0)) {
                count++;
            }
        }
        
        return count;
    }
    
    public static boolean isNewLine(LeafPsiElement psiElement) {
        return psiElement.getElementType() == JetTokens.WHITE_SPACE && psiElement.getText().contains(LINE_SEPARATOR);
    }
    
    public static int getDefaultIndent() {
        return EditorsUI.getPreferenceStore().getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH);
    }
    
    public static boolean isSpacesForTabs() {
        return EditorsUI.getPreferenceStore().getBoolean(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS);
    }
}
