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

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.eclipse.ui.utils.IndenterUtil;

public class KotlinAutoIndentStrategy implements IAutoEditStrategy {
    
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
    
    private void autoEditAfterNewLine(IDocument document, DocumentCommand command) {
        if (command.offset == -1 || document.getLength() == 0) {
            return;
        }
        
        try {
            int p = command.offset == document.getLength() ? command.offset - 1 : command.offset;
            IRegion info = document.getLineInformationOfOffset(p);
            
            int start = info.getOffset();
            
            boolean afterOpenBrace = isAfterOpenBrace(document, command.offset - 1, start);
            boolean beforeCloseBrace = isBeforeCloseBrace(document, command.offset, info.getOffset() + info.getLength());
            int oldOffset = command.offset;
            int newOffset = findEndOfWhiteSpace(document, command.offset - 1) + 1;
            if (newOffset > 0 && !IndenterUtil.isWhiteSpaceOrNewLine(document.getChar(newOffset - 1))) {
                command.offset = newOffset;
                command.length = oldOffset - newOffset;
            } else {
            }
            
            if (beforeCloseBrace && afterOpenBrace) {
                command.caretOffset = command.offset + command.text.length();
                command.shiftsCaret = false;
                command.length += document.get().indexOf(CLOSING_BRACE_CHAR, p) - p;
            }
        } catch (BadLocationException e) {
            KotlinLogger.logAndThrow(e);
        }
    }
    
    private void autoEditBeforeCloseBrace(IDocument document, DocumentCommand command) {
        if (isNewLineBefore(document, command.offset)) {
            try {
                int spaceLength = command.offset - findEndOfWhiteSpaceBefore(document, command.offset - 1, 0) - 1;
                command.offset -= spaceLength;
                document.replace(command.offset, spaceLength, "");
            } catch (BadLocationException e) {
                KotlinLogger.logAndThrow(e);
            }
        }
    }
    
    private static int findEndOfWhiteSpaceAfter(IDocument document, int offset, int end) throws BadLocationException {
        while (offset < end) {
            if (!IndenterUtil.isWhiteSpaceChar(document.getChar(offset))) {
                return offset;
            }
            
            offset++;
        }
        
        return end;
    }
    
    private static int findEndOfWhiteSpaceBefore(IDocument document, int offset, int start) throws BadLocationException {
        while (offset >= start) {
            if (!IndenterUtil.isWhiteSpaceChar(document.getChar(offset))) {
                return offset;
            }
            
            offset--;
        }
        
        return start;
    }
    
    private static boolean isAfterOpenBrace(IDocument document, int offset, int startLineOffset) throws BadLocationException {
        int nonEmptyOffset = findEndOfWhiteSpaceBefore(document, offset, startLineOffset);
        return document.getChar(nonEmptyOffset) == OPENING_BRACE_CHAR;
    }
    
    private static boolean isBeforeCloseBrace(IDocument document, int offset, int endLineOffset) throws BadLocationException {
        int nonEmptyOffset = findEndOfWhiteSpaceAfter(document, offset, endLineOffset);
        if (nonEmptyOffset == document.getLength()) {
            nonEmptyOffset--;
        }
        return document.getChar(nonEmptyOffset) == CLOSING_BRACE_CHAR;
    }
    
    private static int findEndOfWhiteSpace(IDocument document, int offset) throws BadLocationException {
        while (offset > 0) {
            char c = document.getChar(offset);
            if (!IndenterUtil.isWhiteSpaceChar(c)) {
                return offset;
            }
            
            offset--;
        }
        
        return offset;
    }
    
    private static boolean isNewLineBefore(IDocument document, int offset) {
        try {
            offset--;
            char prev = IndenterUtil.SPACE_CHAR;
            StringBuilder bufBefore = new StringBuilder(prev);
            while (IndenterUtil.isWhiteSpaceChar(prev) && offset > 0) {
                prev = document.getChar(offset--);
                bufBefore.append(prev);
            }
            
            return containsNewLine(document, bufBefore.toString());
        } catch (BadLocationException e) {
            KotlinLogger.logAndThrow(e);
        }
        
        return false;
    }
    
    private static boolean containsNewLine(IDocument document, String text) {
        String[] delimiters = document.getLegalLineDelimiters();
        for (String delimiter : delimiters) {
            if (text.contains(delimiter)) {
                return true;
            }
        }
        
        return false;
    }
    
    private static boolean isNewLine(IDocument document, String text) {
        String[] delimiters = document.getLegalLineDelimiters();
        for (String delimiter : delimiters) {
            if (delimiter.equals(text)) {
                return true;
            }
        }
        
        return false;
    }
}