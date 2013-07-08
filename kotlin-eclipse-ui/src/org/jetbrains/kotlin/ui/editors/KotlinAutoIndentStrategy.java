package org.jetbrains.kotlin.ui.editors;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.jetbrains.kotlin.parser.KotlinParser;
import org.jetbrains.kotlin.utils.IndenterUtil;

import com.intellij.lang.ASTNode;

public class KotlinAutoIndentStrategy implements IAutoEditStrategy {
    
    public static final Set<String> BLOCK_ELEMENT_TYPES = new HashSet<String>(Arrays.asList("IF", "FOR", "WHILE", "FUN", "CLASS", "FUNCTION_LITERAL_EXPRESSION", "WHEN"));
    
    @Override
    public void customizeDocumentCommand(IDocument document, DocumentCommand command) {
        if (command.doit == false) {
            return;
        }
        
        if (command.length == 0 && command.text != null && isNewLine(document, command.text)) {
            autoEditAfterNewLine(document, command);
        } else if ("}".equals(command.text)) {
            autoEditBeforeCloseBrace(document, command);
        }
    }
    
    private void autoEditBeforeCloseBrace(IDocument document, DocumentCommand command) {
        if (isNewLineBefore(document, command.offset)) {
            int indent = computeIndentByOffset(document, command.offset);
            command.offset -= indent * IndenterUtil.getDefaultIndent();
            command.text = IndenterUtil.createWhiteSpace(indent - 1, 0) + "}";
        }
    }
    
    private int findEndOfWhiteSpaceAfter(IDocument document, int offset, int end) throws BadLocationException {
        while (offset < end) {
            char c = document.getChar(offset);
            if (c != ' ' && c != '\t') {
                return offset;
            }
            offset++;
        }
        return end;
    }
    
    private int findEndOfWhiteSpaceBefore(IDocument document, int offset, int start) throws BadLocationException {
        while (offset >= start) {
            char c = document.getChar(offset);
            if (c != ' ' && c != '\t') {
                return offset;
            }
            offset--;
        }
        return start;
    }
    
    private boolean isAfterOpenBrace(IDocument document, int offset, int startLineOffset) throws BadLocationException {
        int nonEmptyOffset = findEndOfWhiteSpaceBefore(document, offset, startLineOffset);
        return document.getChar(nonEmptyOffset) == '{';
    }
    
    private boolean isBeforeCloseBrace(IDocument document, int offset, int endLineOffset) throws BadLocationException {
        int nonEmptyOffset = findEndOfWhiteSpaceAfter(document, offset, endLineOffset);
        if (nonEmptyOffset == document.getLength()) {
            nonEmptyOffset--;
        }
        return document.getChar(nonEmptyOffset) == '}';
    }
    
    private void autoEditAfterNewLine(IDocument document, DocumentCommand command) {
        if (command.offset == -1 || document.getLength() == 0) {
            return;
        }
        
        try {
            int p = command.offset == document.getLength() ? command.offset - 1 : command.offset;
            IRegion info = document.getLineInformationOfOffset(p);
            int start = info.getOffset();
            
            StringBuffer buf = new StringBuffer(command.text);
            
            int end = findEndOfWhiteSpaceAfter(document, start, command.offset);
            
            String lineSpaces = (end > start) ? document.get(start, end - start) : ""; 
            buf.append(lineSpaces);
            
            if (isAfterOpenBrace(document, command.offset - 1, start)) {                            
                buf.append(IndenterUtil.createWhiteSpace(1, 0));
                
                if (isBeforeCloseBrace(document, command.offset, info.getOffset() + info.getLength())) {
                    command.shiftsCaret = false;
                    command.caretOffset = command.offset + buf.length();
                    
                    buf.append(command.text);
                    buf.append(lineSpaces);                                 
                }
                command.text = buf.toString();
            } else {
                int indent = computeIndentByOffset(document, command.offset);
                if (isBeforeCloseBrace(document, command.offset, info.getOffset() + info.getLength())) {
                    indent--;
                }
                command.text += IndenterUtil.createWhiteSpace(indent, 0);
           }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
    
    private int computeIndentByOffset(IDocument document, int offset) {
        try {
            if (offset == document.getLength()) {
                return 0;
            }
            
            ASTNode parsedDocument = KotlinParser.parseText(document.get());
            if (document.get().contains("\r")) {
                offset -= document.getLineOfOffset(offset);
            }
            ASTNode leaf = parsedDocument.findLeafElementAt(offset);
            int indent = 0;
            while(leaf != null) {
                if (BLOCK_ELEMENT_TYPES.contains(leaf.getElementType().toString())) {
                    indent++;
                }
                leaf = leaf.getTreeParent();
            }
            
            return indent;
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        
        return 0; 
    }
    
    private boolean isNewLineBefore(IDocument document, int offset) {
        try {
            offset--;
            String prev = " ";
            StringBuilder bufBefore = new StringBuilder(prev);
            while ((" ".equals(prev) || "\t".equals(prev)) && offset > 0) {
                prev = document.get(offset--, 1);
                bufBefore.append(prev);
            }
            
            return containsNewLine(document, bufBefore.toString());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        
        return false;
    }
    
    private boolean containsNewLine(IDocument document, String text) {
        String[] delimiters = document.getLegalLineDelimiters();
        for (String delimiter : delimiters) {
            if (text.contains(delimiter)) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean isNewLine(IDocument document, String text) {
        String[] delimiters = document.getLegalLineDelimiters();
        for (String delimiter : delimiters) {
            if (delimiter.equals(text)) {
                return true;
            }
        }
        
        return false;
    }
}
