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
import org.jetbrains.kotlin.psi.JetClass;
import org.jetbrains.kotlin.psi.JetClassInitializer;
import org.jetbrains.kotlin.psi.JetFunction;
import org.jetbrains.kotlin.psi.JetImportList;
import org.jetbrains.kotlin.psi.JetPackageDirective;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;

public class AlignmentStrategy {
    
    private final ASTNode parsedFile;
    private StringBuilder edit;
    private final int lineIndentation;
    private final String lineDelimiter;
    
    private static final Set<String> BLOCK_ELEMENT_TYPES = new HashSet<String>(Arrays.asList(
            "IF", "FOR", "WHILE", "CLASS_BODY", "FUNCTION_LITERAL_EXPRESSION", "PROPERTY", "WHEN"));
    
    public AlignmentStrategy(ASTNode parsedFile, int lineIndentation, String lineDelimiter) {
        this.parsedFile = parsedFile;
        this.lineIndentation = lineIndentation;
        this.lineDelimiter = lineDelimiter;
    }
    
    public String placeSpaces() {
        edit = new StringBuilder();
        buildFormattedCode(parsedFile, lineIndentation);
        
        return edit.toString();
    }
    
    private void buildFormattedCode(ASTNode node, int indent) {
        indent = updateIndent(node, indent);
        
        PsiElement psiElement = node.getPsi();
        if (psiElement instanceof LeafPsiElement) {
            String text = psiElement.getText();
            if (IndenterUtil.isNewLine((LeafPsiElement) psiElement)) {
                int shift = indent;
                if (isBrace(psiElement.getNextSibling())) {
                    shift--;
                }
                
                if (isEmptyLineNeeded(psiElement)) {
                    edit.append(IndenterUtil.createWhiteSpace(shift, 1, lineDelimiter));
                }
                
                int lineSeparatorsOccurences = IndenterUtil.getLineSeparatorsOccurences(text);
                edit.append(IndenterUtil.createWhiteSpace(shift, lineSeparatorsOccurences, lineDelimiter));
            } else {
                if (isWhiteSpaceNeeded(psiElement)) {
                    edit.append(" ");
                }
                
                String textWithDefaultIndents = text.replaceAll(LineEndUtil.NEW_LINE_STRING, lineDelimiter);
                if (IndenterUtil.isSpacesForTabs()) {
                    textWithDefaultIndents = textWithDefaultIndents.replaceAll(IndenterUtil.TAB_STRING, IndenterUtil.getIndentString());
                }
                edit.append(textWithDefaultIndents);
            }
        }
        
        for (ASTNode child : node.getChildren(null)) {
            buildFormattedCode(child, indent);
        }
    }
    
    private boolean isWhiteSpaceNeeded(PsiElement psiElement) {
        if (isLBrace(psiElement)) {
            if (edit.length() > 1) {
                return !IndenterUtil.isWhiteSpaceOrNewLine(edit.charAt(edit.length() - 1));
            }
        }
        
        return false;
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
    
    private static boolean isLBrace(PsiElement psiElement) {
        LeafPsiElement leafPsiElement = getFirstLeaf(psiElement);
        
        if (leafPsiElement != null) {
            IElementType elementType = leafPsiElement.getElementType();
            if (elementType == JetTokens.LBRACE) {
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
    
    public static String alignCode(ASTNode parsedFile, String lineDelimiter) {
        return alignCode(parsedFile, 0, lineDelimiter);
    }
    
    public static String alignCode(ASTNode parsedFile, int lineIndentation, String lineDelimiter) {
        return new AlignmentStrategy(parsedFile, lineIndentation, lineDelimiter).placeSpaces();
    }
    
    public static int computeIndent(ASTNode node) {
        int indent = 0;
        while(node != null) {
            indent = AlignmentStrategy.updateIndent(node, indent);
            node = node.getTreeParent();
        }
        
        return indent;
    }
    
    private boolean isEmptyLineNeeded(PsiElement psiElement) {
        if (IndenterUtil.getLineSeparatorsOccurences(psiElement.getText()) > 1) {
            return false;
        }
        
        PsiElement prevSibling = psiElement.getPrevSibling();
        PsiElement nextSibling = psiElement.getNextSibling();
        
        if (prevSibling == null || nextSibling == null) return false;
        
        if (prevSibling instanceof JetPackageDirective) {
            return !prevSibling.getText().isEmpty();
        }
        
        if (prevSibling instanceof JetImportList) {
            return !prevSibling.getText().isEmpty();
        }
        
        return false;
    }
    
    public static int updateIndent(ASTNode node, int indent) {
        IElementType elementType = node.getElementType();
        if ("BLOCK".equals(elementType.toString())) {
            PsiElement parent = node.getPsi().getParent();
            if (parent instanceof JetFunction || parent instanceof JetClass || parent instanceof JetClassInitializer) {
                return indent + 1;
            } 
        } else if (BLOCK_ELEMENT_TYPES.contains(elementType.toString())) {
            return indent + 1;
        }
        
        return indent;
    }
}
