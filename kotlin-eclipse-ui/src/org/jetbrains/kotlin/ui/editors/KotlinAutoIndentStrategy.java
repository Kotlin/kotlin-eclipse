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
package org.jetbrains.kotlin.ui.editors;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.utils.EditorUtil;
import org.jetbrains.kotlin.utils.IndenterUtil;
import org.jetbrains.kotlin.utils.LineEndUtil;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

public class KotlinAutoIndentStrategy implements IAutoEditStrategy {
    
    public static final Set<String> BLOCK_ELEMENT_TYPES = new HashSet<String>(Arrays.asList("IF", "FOR", "WHILE", "FUN", "CLASS", "FUNCTION_LITERAL_EXPRESSION", "WHEN"));
    
    private static final char OPENING_BRACE_CHAR = '{';
    private static final char CLOSING_BRACE_CHAR = '}';
    private static final String CLOSING_BRACE_STRING = Character.toString(CLOSING_BRACE_CHAR);
    
    private final JavaEditor editor;
    
    public KotlinAutoIndentStrategy(JavaEditor editor) {
        this.editor = editor;
    }
    
    @Override
    public void customizeDocumentCommand(IDocument document, DocumentCommand command) {
        if (command.doit == false) {
            return;
        }
        
        if (command.length == 0 && command.text != null && isNewLine(document, command.text)) {
            autoEditAfterNewLine(document, command);
        } else if (CLOSING_BRACE_STRING.equals(command.text)) {
            autoEditBeforeCloseBrace(document, command);
        }
    }
    
    private void autoEditBeforeCloseBrace(IDocument document, DocumentCommand command) {
        if (isNewLineBefore(document, command.offset)) {
            try {
                int indent = IndenterUtil.getDefaultIndent();
                                
                while (indent > 0) {
                    char c = document.getChar(command.offset - 1);
                    if (c == IndenterUtil.SPACE_CHAR) {
                        indent--;
                    } else if (c == IndenterUtil.TAB_CHAR) {
                        indent -= IndenterUtil.getDefaultIndent();
                    } else {
                        break;
                    }
                    
                    command.offset--;
                    document.replace(command.offset, 1, "");
                }
            } catch (BadLocationException e) {
                KotlinLogger.logAndThrow(e);
            }
            
            command.text = CLOSING_BRACE_STRING;
        }
    }
    
    private int findEndOfWhiteSpaceAfter(IDocument document, int offset, int end) throws BadLocationException {
        while (offset < end) {
            char c = document.getChar(offset);
            if (c != IndenterUtil.SPACE_CHAR && c != IndenterUtil.TAB_CHAR) {
                return offset;
            }
            offset++;
        }
        return end;
    }
    
    private int findEndOfWhiteSpaceBefore(IDocument document, int offset, int start) throws BadLocationException {
        while (offset >= start) {
            char c = document.getChar(offset);
            if (c != IndenterUtil.SPACE_CHAR && c != IndenterUtil.TAB_CHAR) {
                return offset;
            }
            offset--;
        }
        return start;
    }
    
    private boolean isAfterOpenBrace(IDocument document, int offset, int startLineOffset) throws BadLocationException {
        int nonEmptyOffset = findEndOfWhiteSpaceBefore(document, offset, startLineOffset);
        return document.getChar(nonEmptyOffset) == OPENING_BRACE_CHAR;
    }
    
    private boolean isBeforeCloseBrace(IDocument document, int offset, int endLineOffset) throws BadLocationException {
        int nonEmptyOffset = findEndOfWhiteSpaceAfter(document, offset, endLineOffset);
        if (nonEmptyOffset == document.getLength()) {
            nonEmptyOffset--;
        }
        return document.getChar(nonEmptyOffset) == CLOSING_BRACE_CHAR;
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
            KotlinLogger.logAndThrow(e);
        }
    }
    
    private int computeIndentByOffset(IDocument document, int offset) {
        try {
            if (offset == document.getLength()) {
                return 0;
            }
            
            IFile file = EditorUtil.getFile(editor);
            PsiFile parsedDocument = KotlinPsiManager.INSTANCE.getParsedFile(file, document.get());
            if (document.get().contains(LineEndUtil.CARRIAGE_RETURN_STRING)) {
                offset -= document.getLineOfOffset(offset);
            }
            
            PsiElement leaf = parsedDocument.findElementAt(offset);
            if (leaf == null) {
                return 0;
            }
            
            if (leaf.getNode().getElementType() != JetTokens.WHITE_SPACE) {
                leaf = parsedDocument.findElementAt(offset - 1);
            }
            
            int indent = 0;
            
            ASTNode node = null;
            if (leaf != null) {
                node = leaf.getNode();
            }
            while(node != null) {
                if (BLOCK_ELEMENT_TYPES.contains(node.getElementType().toString())) {
                    indent++;
                }
                node = node.getTreeParent();
            }
            
            return indent;
        } catch (BadLocationException e) {
            KotlinLogger.logAndThrow(e);
        }
        
        return 0; 
    }
    
    private boolean isNewLineBefore(IDocument document, int offset) {
        try {
            offset--;
            String prev = IndenterUtil.SPACE_STRING;
            StringBuilder bufBefore = new StringBuilder(prev);
            while ((prev.equals(IndenterUtil.SPACE_STRING) || prev.equals(IndenterUtil.TAB_STRING)) && offset > 0) {
                prev = document.get(offset--, 1);
                bufBefore.append(prev);
            }
            
            return containsNewLine(document, bufBefore.toString());
        } catch (BadLocationException e) {
            KotlinLogger.logAndThrow(e);
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
