package org.jetbrains.kotlin.ui.formatter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.formatting.FormattingDocumentModel;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.formatter.WhiteSpaceFormattingStrategy;
import com.intellij.psi.formatter.WhiteSpaceFormattingStrategyFactory;

public class EclipseFormattingModel implements FormattingDocumentModel {
    
    private final WhiteSpaceFormattingStrategy myWhiteSpaceStrategy;
    // private final CharBuffer myBuffer = CharBuffer.allocate(1);
    @NotNull
    private final Document myDocument;
    private final PsiFile myFile;
    
    private static final Logger LOG = Logger.getInstance("#com.intellij.psi.formatter.FormattingDocumentModelImpl");
    private final CodeStyleSettings mySettings;
    
    private final int maxDepthToCheck = 10;
    private final String applyChangeStateClassName = "com.intellij.formatting.FormatProcessor$ApplyChangesState";
    private final String prepareMethodName = "prepare";
    
    private final boolean forLineIndentation;
    
    public EclipseFormattingModel(
            @NotNull final Document document, 
            PsiFile file, 
            CodeStyleSettings settings,
            boolean forLineIndentation) {
        myDocument = document;
        myFile = file;
        if (file != null) {
            Language language = file.getLanguage();
            myWhiteSpaceStrategy = WhiteSpaceFormattingStrategyFactory.getStrategy(language);
        } else {
            myWhiteSpaceStrategy = WhiteSpaceFormattingStrategyFactory.getStrategy();
        }
        mySettings = settings;
        this.forLineIndentation = forLineIndentation;
    }
    
    @Nullable
    public static Document getDocumentToBeUsedFor(final PsiFile file) {
        final Project project = file.getProject();
        final Document document = PsiDocumentManager.getInstance(project).getDocument(file);
        if (document == null)
            return null;
        if (PsiDocumentManager.getInstance(project).isUncommited(document))
            return null;
        
        return document;
    }
    
    @Override
    public int getLineNumber(int offset) {
        if (offset > myDocument.getTextLength()) {
            LOG.error(String.format("Invalid offset detected (%d). Document length: %d. Target file: %s", offset,
                    myDocument.getTextLength(), myFile));
        }
        return myDocument.getLineNumber(offset);
    }
    
    @Override
    public int getLineStartOffset(int line) {
        return myDocument.getLineStartOffset(line);
    }
    
    @Override
    public CharSequence getText(final TextRange textRange) {
        if (textRange.getStartOffset() < 0 || textRange.getEndOffset() > myDocument.getTextLength()) {
            LOG.error(String.format(
                    "Please submit a ticket to the tracker and attach current source file to it!%nInvalid processing detected: given text "
                            + "range (%s) targets non-existing regions (the boundaries are [0; %d)). File's language: %s",
                    textRange, myDocument.getTextLength(), myFile.getLanguage()));
        }
        return myDocument.getCharsSequence().subSequence(textRange.getStartOffset(), textRange.getEndOffset());
    }
    
    @Override
    public int getTextLength() {
        return myDocument.getTextLength();
    }
    
    @NotNull
    @Override
    public Document getDocument() {
        if (forLineIndentation) { // Optimization to avoid getStackTrace() calls
            return myDocument;
        }
        
        // In the compiler we have shrinked version of DocumentEx interface which doesn't have setInBulkUpdate method
        // This workaround allows us to avoid place in FormatProcessor where setInBulkUpdate is called 
        int i = 0;
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            if (i > maxDepthToCheck) {
                break;
            }
            
            if (prepareMethodName.equals(element.getMethodName()) && applyChangeStateClassName.equals(element.getClassName())) {
                return KotlinFormatterKt.getMockDocument(myDocument);
            }
            
            i++;
        }
        return myDocument;
    }
    
    public PsiFile getFile() {
        return myFile;
    }
    
    @Override
    public boolean containsWhiteSpaceSymbolsOnly(int startOffset, int endOffset) {
        WhiteSpaceFormattingStrategy strategy = myWhiteSpaceStrategy;
        if (strategy.check(myDocument.getCharsSequence(), startOffset, endOffset) >= endOffset) {
            return true;
        }
        return false;
    }
    
    @NotNull
    @Override
    public CharSequence adjustWhiteSpaceIfNecessary(@NotNull CharSequence whiteSpaceText, int startOffset,
            int endOffset, @Nullable ASTNode nodeAfter, boolean changedViaPsi) {
        if (!changedViaPsi) {
            return myWhiteSpaceStrategy.adjustWhiteSpaceIfNecessary(whiteSpaceText, myDocument.getCharsSequence(),
                    startOffset, endOffset, mySettings, nodeAfter);
        }
        
        final PsiElement element = myFile.findElementAt(startOffset);
        if (element == null) {
            return whiteSpaceText;
        } else {
            return myWhiteSpaceStrategy.adjustWhiteSpaceIfNecessary(whiteSpaceText, element, startOffset, endOffset,
                    mySettings);
        }
    }
    
    // @Override
    // public boolean isWhiteSpaceSymbol(char symbol) {
    // myBuffer.put(0, symbol);
    // return myWhiteSpaceStrategy.check(myBuffer, 0, 1) > 0;
    // }
    
    public static boolean canUseDocumentModel(@NotNull Document document, @NotNull PsiFile file) {
        PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(file.getProject());
        return !psiDocumentManager.isUncommited(document) && !psiDocumentManager.isDocumentBlockedByPsi(document)
                && file.getText().equals(document.getText());
    }
}