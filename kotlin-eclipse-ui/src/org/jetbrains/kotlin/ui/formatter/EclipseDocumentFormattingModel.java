package org.jetbrains.kotlin.ui.formatter;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.ltk.core.refactoring.DocumentChange;
import org.eclipse.text.edits.ReplaceEdit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil;

import com.intellij.formatting.Block;
import com.intellij.formatting.FormattingDocumentModel;
import com.intellij.formatting.FormattingModelEx;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;

public class EclipseDocumentFormattingModel implements FormattingModelEx {
    
    private final Project myProject;
    private final ASTNode myASTNode;
    private final EclipseFormattingModel myDocumentModel;
    @NotNull
    private final Block myRootBlock;
    protected boolean myCanModifyAllWhiteSpaces = false;
    private final IDocument document;
    private final List<ReplaceEdit> edits = new ArrayList<>();
    private final CodeStyleSettings settings;
    
    public EclipseDocumentFormattingModel(final PsiFile file, @NotNull final Block rootBlock,
            final EclipseFormattingModel documentModel, @NotNull final IDocument document,
            @NotNull CodeStyleSettings settings) {
        myASTNode = SourceTreeToPsiMap.psiElementToTree(file);
        myDocumentModel = documentModel;
        myRootBlock = rootBlock;
        myProject = file.getProject();
        this.document = document;
        this.settings = settings;
    }
    
    @Override
    @NotNull
    public Block getRootBlock() {
        return myRootBlock;
    }
    
    @Override
    @NotNull
    public FormattingDocumentModel getDocumentModel() {
        return myDocumentModel;
    }
    
    @Override
    public TextRange replaceWhiteSpace(TextRange textRange, String whiteSpace) {
        return replaceWhiteSpace(textRange, null, whiteSpace);
    }
    
    @Override
    public TextRange replaceWhiteSpace(TextRange textRange, @Nullable ASTNode nodeAfter, String whiteSpace) {
        CharSequence whiteSpaceToUse = getDocumentModel().adjustWhiteSpaceIfNecessary(whiteSpace,
                textRange.getStartOffset(), textRange.getEndOffset(), nodeAfter, false);
        
        replace(textRange, whiteSpaceToUse.toString());
        return textRange;
    }
    
    @Override
    public TextRange shiftIndentInsideRange(ASTNode node, TextRange range, int indent) {
        try {
            int newLength = shiftIndentInside(range, indent);
            return new TextRange(range.getStartOffset(), range.getStartOffset() + newLength);
        } catch (BadLocationException e) {
            KotlinLogger.logAndThrow(e);
        }
        
        return null;
    }
    
    @Override
    public void commitChanges() {
        DocumentChange documentChange = new DocumentChange("Format code", document);
        for (ReplaceEdit edit : edits) {
            TextChangeCompatibility.addTextEdit(documentChange, "Kotlin change", edit);
        }
        
        try {
            documentChange.perform(new NullProgressMonitor());
        } catch (CoreException e) {
            KotlinLogger.logError(e);
        }
    }
    
    private int shiftIndentInside(final TextRange elementRange, final int shift) throws BadLocationException {
        final StringBuilder buffer = new StringBuilder();
        StringBuilder afterWhiteSpace = new StringBuilder();
        int whiteSpaceLength = 0;
        boolean insideWhiteSpace = true;
        int line = 0;
        for (int i = elementRange.getStartOffset(); i < elementRange.getEndOffset(); i++) {
            final char c = document.getChar(i);
            switch (c) {
            case '\n':
                if (line > 0) {
                    createWhiteSpace(whiteSpaceLength + shift, buffer);
                }
                buffer.append(afterWhiteSpace.toString());
                insideWhiteSpace = true;
                whiteSpaceLength = 0;
                afterWhiteSpace = new StringBuilder();
                buffer.append(c);
                line++;
                break;
            case ' ':
                if (insideWhiteSpace) {
                    whiteSpaceLength += 1;
                } else {
                    afterWhiteSpace.append(c);
                }
                break;
            case '\t':
                if (insideWhiteSpace) {
                    whiteSpaceLength += getIndentOptions().TAB_SIZE;
                } else {
                    afterWhiteSpace.append(c);
                }
                
                break;
            default:
                insideWhiteSpace = false;
                afterWhiteSpace.append(c);
            }
        }
        if (line > 0) {
            createWhiteSpace(whiteSpaceLength + shift, buffer);
        }
        buffer.append(afterWhiteSpace.toString());
        
        replace(elementRange, buffer.toString());
        
        return buffer.length();
    }
    
    private void createWhiteSpace(final int whiteSpaceLength, StringBuilder buffer) {
        if (whiteSpaceLength < 0)
            return;
        
        // TODO: create whitespace with tabs if needed
        StringUtil.repeatSymbol(buffer, ' ', whiteSpaceLength);
    }
    
    private CommonCodeStyleSettings.IndentOptions getIndentOptions() {
        return settings.getIndentOptions();
    }
    
    public Project getProject() {
        return myProject;
    }
    
    @Nullable
    public static String mergeWsWithCdataMarker(String whiteSpace, final String s, final int cdataPos) {
        final int firstCrInGeneratedWs = whiteSpace.indexOf('\n');
        final int secondCrInGeneratedWs = firstCrInGeneratedWs != -1
                ? whiteSpace.indexOf('\n', firstCrInGeneratedWs + 1) : -1;
        final int firstCrInPreviousWs = s.indexOf('\n');
        final int secondCrInPreviousWs = firstCrInPreviousWs != -1 ? s.indexOf('\n', firstCrInPreviousWs + 1) : -1;
        
        boolean knowHowToModifyCData = false;
        
        if (secondCrInPreviousWs != -1 && secondCrInGeneratedWs != -1 && cdataPos > firstCrInPreviousWs
                && cdataPos < secondCrInPreviousWs) {
            whiteSpace = whiteSpace.substring(0, secondCrInGeneratedWs)
                    + s.substring(firstCrInPreviousWs + 1, secondCrInPreviousWs)
                    + whiteSpace.substring(secondCrInGeneratedWs);
            knowHowToModifyCData = true;
        }
        if (!knowHowToModifyCData)
            whiteSpace = null;
        return whiteSpace;
    }
    
    private void replace(TextRange range, String whiteSpace) {
        String convertedWhiteSpace = StringUtil.convertLineSeparators(whiteSpace, TextUtilities.getDefaultLineDelimiter(document));
        int startOffset = convertOffset(range.getStartOffset());
        int endOffset = convertOffset(range.getEndOffset());
        ReplaceEdit edit = new ReplaceEdit(startOffset, endOffset - startOffset, convertedWhiteSpace);
        edits.add(edit);
    }
    
    private int convertOffset(int offset) {
        return LineEndUtil.convertLfToDocumentOffset(myASTNode.getPsi().getContainingFile().getText(), offset,
                document);
    }
}