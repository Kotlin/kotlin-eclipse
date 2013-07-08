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
    
    public static final Set<String> blockElementTypes;
    private static final String lineSeparator = "\n";
    
    static {
        blockElementTypes = new HashSet<String>(Arrays.asList("IF", "FOR", "WHILE", "FUN", "CLASS", "FUNCTION_LITERAL_EXPRESSION", "PROPERTY", "WHEN"));
    }
    
    public AlignmentStrategy(ASTNode parsedFile) {
        this.parsedFile = parsedFile;
    }
    
    public String placeSpaces() {
        edit = new StringBuilder();
        buildFormattedCode(parsedFile, 0);
        
        return edit.toString();
    }
    
    public static String alignCode(ASTNode parsedFile) {
        return new AlignmentStrategy(parsedFile).placeSpaces();
    }
    
    private void buildFormattedCode(ASTNode node, int indent) {
        indent = updateIndent(node, indent);
        for (ASTNode child : node.getChildren(null)) {
            PsiElement psiElement = child.getPsi();
            
            if (psiElement instanceof LeafPsiElement) {
                if (isNewLine((LeafPsiElement) psiElement)) {
                    int shift = indent;
                    if (isBrace(psiElement.getNextSibling())) {
                        shift--;
                    }
                    
                    edit.append(IndenterUtil.createWhiteSpace(shift, getLineSeparatorsOccurences(psiElement.getText())));
                } else {
                    edit.append(psiElement.getText());
                }
            }
            buildFormattedCode(child, indent);
        }
    }
    
    private boolean isNewLine(LeafPsiElement psiElement) {
        return psiElement.getElementType() == JetTokens.WHITE_SPACE && psiElement.getText().contains(lineSeparator);
    }
    
    private int getLineSeparatorsOccurences(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); ++i) {
            if (text.charAt(i) == lineSeparator.charAt(0)) {
                count++;
            }
        }
        
        return count;
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