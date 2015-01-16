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

import org.jetbrains.kotlin.eclipse.ui.utils.IndenterUtil;
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil;
import org.jetbrains.kotlin.lexer.JetTokens;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;

public class AlignmentStrategy {
    
    private final ASTNode parsedFile;
    private StringBuilder edit;
    private final int lineIndentation;
    
    private static final Set<String> BLOCK_ELEMENT_TYPES = new HashSet<String>(Arrays.asList(
            "IF", "FOR", "WHILE", "FUN", "CLASS", "OBJECT_DECLARATION",
            "FUNCTION_LITERAL_EXPRESSION", "PROPERTY", "WHEN"));
    
    public AlignmentStrategy(ASTNode parsedFile, int lineIndentation) {
        this.parsedFile = parsedFile;
        this.lineIndentation = lineIndentation;
    }
    
    public String placeSpaces() {
        edit = new StringBuilder();
        buildFormattedCode(parsedFile, lineIndentation);
        
        return LineEndUtil.replaceAllNewLinesWithSystemLineSeparators(edit.toString());
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
                    
                    int lineSeparatorsOccurences = IndenterUtil.getLineSeparatorsOccurences(psiElement.getText());
                    edit.append(IndenterUtil.createWhiteSpace(shift, lineSeparatorsOccurences, LineEndUtil.NEW_LINE_STRING));
                } else {
                    edit.append(psiElement.getText());
                }
            }
            
            buildFormattedCode(child, indent);
        }
    }
    
    private static boolean isBrace(PsiElement psiElement) {
        LeafPsiElement leafPsiElement = getFirstLeaf(psiElement);
        
        if (leafPsiElement != null) {
            IElementType elementType = leafPsiElement.getElementType();
            if (elementType == JetTokens.LBRACE || elementType == JetTokens.RBRACE) {
                return true;
            }
        }
        
        return false;
    }
    
    private static LeafPsiElement getFirstLeaf(PsiElement psiElement) {
        PsiElement child = psiElement;
        
        while (true) {
            if (child instanceof LeafPsiElement || child == null) {
                return (LeafPsiElement) child;
            }
            
            child = child.getFirstChild();
        }
    }
    
    public static String alignCode(ASTNode parsedFile) {
        return alignCode(parsedFile, 0);
    }
    
    public static String alignCode(ASTNode parsedFile, int lineIndentation) {
        return new AlignmentStrategy(parsedFile, lineIndentation).placeSpaces();
    }
    
    public static int updateIndent(ASTNode node, int indent) {
        if (BLOCK_ELEMENT_TYPES.contains(node.getElementType().toString())) {
            return indent + 1;
        }
        
        return indent;
    }
}