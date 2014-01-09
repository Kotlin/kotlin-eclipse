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
package org.jetbrains.kotlin.ui.formatter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.kotlin.utils.IndenterUtil;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;

public class AlignmentStrategy {
    
    private final ASTNode parsedFile;
    private StringBuilder edit;
    private final int lineIndentation;
    
    public static final Set<String> blockElementTypes;
    
    static {
        blockElementTypes = new HashSet<String>(Arrays.asList("IF", "FOR", "WHILE", "FUN", "CLASS", "FUNCTION_LITERAL_EXPRESSION", "PROPERTY", "WHEN"));
    }
    
    public AlignmentStrategy(ASTNode parsedFile, int lineIndentation) {
        this.parsedFile = parsedFile;
        this.lineIndentation = lineIndentation;
    }
    
    public String placeSpaces() {
        edit = new StringBuilder();
        buildFormattedCode(parsedFile, lineIndentation);
        
        String editString = edit.toString();
        editString = editString.replaceAll("\n", System.lineSeparator());
        
        return editString;
    }
    
    public static String alignCode(ASTNode parsedFile) {
        return new AlignmentStrategy(parsedFile, 0).placeSpaces();
    }
    
    public static String alignCode(ASTNode parsedFile, int lineIndentation) {
        return new AlignmentStrategy(parsedFile, lineIndentation).placeSpaces();
    }
    
    private void buildFormattedCode(ASTNode node, int indent) {
        indent = updateIndent(node, indent);
        for (ASTNode child : node.getChildren(null)) {
            PsiElement psiElement = child.getPsi();
            
            if (psiElement instanceof LeafPsiElement) {
                if (IndenterUtil.isNewLine((LeafPsiElement) psiElement)) {
                    int shift = indent;
                    if (isBrace(psiElement.getNextSibling())) {
                        shift--;
                    }
                    
                    edit.append(IndenterUtil.createWhiteSpace(shift, IndenterUtil.getLineSeparatorsOccurences(psiElement.getText()), "\n"));
                } else {
                    edit.append(psiElement.getText());
                }
            }
            buildFormattedCode(child, indent);
        }
    }
    
    private boolean isBrace(PsiElement psiElement) {
        LeafPsiElement leafPsiElement = getFirstLeaf(psiElement);
        if (leafPsiElement != null) {
            IElementType elementType = leafPsiElement.getElementType();
            if (elementType == JetTokens.LBRACE || elementType == JetTokens.RBRACE) {
                return true;
            }
        }
        
        return false;   
    }
    
    private LeafPsiElement getFirstLeaf(PsiElement psiElement) {
        PsiElement child = psiElement;
        while (true) {
            if (child instanceof LeafPsiElement || child == null) {
                return (LeafPsiElement) child;
            }
            child = child.getFirstChild();
        }
    }
    
    private int updateIndent(ASTNode node, int curIndent) {
        if (blockElementTypes.contains(node.getElementType().toString())) {
            return curIndent + 1;
        } 
        
        return curIndent;
    }
}