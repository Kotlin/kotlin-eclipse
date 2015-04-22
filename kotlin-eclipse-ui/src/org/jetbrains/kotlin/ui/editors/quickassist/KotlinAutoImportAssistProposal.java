package org.jetbrains.kotlin.ui.editors.quickassist;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil;
import org.jetbrains.kotlin.eclipse.ui.utils.IndenterUtil;
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.psi.JetImportDirective;
import org.jetbrains.kotlin.psi.JetPackageDirective;
import org.jetbrains.kotlin.ui.editors.KotlinEditor;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

public class KotlinAutoImportAssistProposal extends KotlinQuickAssistProposal {
    private final IType proposalType;
    
    public KotlinAutoImportAssistProposal(@NotNull IType proposalType) {
        this.proposalType = proposalType;
    }

    @Override
    public void apply(@NotNull IDocument document, @NotNull PsiElement psiElement) {
        KotlinEditor editor = getActiveEditor();
        if (editor == null) {
            return;
        }
        
        IFile file = EditorUtil.getFile(editor);
        if (file == null) {
            KotlinLogger.logError("Failed to retrieve IFile from editor " + editor, null);
            return;
        }

        PsiElement placeElement = findNodeToNewImport(file);
        int breakLineBefore = computeBreakLineBeforeImport(placeElement);
        int breakLineAfter = computeBreakLineAfterImport(placeElement);
        
        String newImport = "import " + proposalType.getFullyQualifiedName('.');
        String lineDelimiter = TextUtilities.getDefaultLineDelimiter(document);
        newImport = IndenterUtil.createWhiteSpace(0, breakLineBefore, lineDelimiter) + newImport + 
                IndenterUtil.createWhiteSpace(0, breakLineAfter, lineDelimiter);
        try {
            document.replace(getOffset(placeElement, editor), 0, newImport);
        } catch (BadLocationException e) {
            KotlinLogger.logError(e);
        } 
        
    }

    @Override
    public boolean isApplicable(@NotNull PsiElement psiElement) {
        return true;
    }
    
    @Override
    public Image getImage() {
        return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_IMPDECL);
    }

    @Override
    public String getDisplayString() {
        return "Import '" + proposalType.getElementName() + "' (" + proposalType.getPackageFragment().getElementName() + ")";
    }
    
    @NotNull
    public String getFqName() {
        return proposalType.getFullyQualifiedName('.');
    }
    
    private int getOffset(PsiElement element, AbstractTextEditor editor) {
        int offset = 0;
        if (element != null) {
            IFile file = EditorUtil.getFile(editor);
            if (file != null) {
                PsiFile parsedFile = KotlinPsiManager.INSTANCE.getParsedFile(file);
                offset = LineEndUtil.convertLfToDocumentOffset(parsedFile.getText(), element.getTextRange().getEndOffset(), EditorUtil.getDocument(editor));
            } else {
                KotlinLogger.logError("Failed to retrieve IFile from editor " + editor, null);
            }
        }
        
        return offset;
    }
    
    private int computeBreakLineAfterImport(PsiElement element) {
        int countBreakLine = 0;
        if (element instanceof JetPackageDirective) {
            ASTNode nextNode = element.getNode().getTreeNext();
            if (nextNode.getElementType().equals(JetTokens.WHITE_SPACE)) {
                int countBreakLineAfterHeader = IndenterUtil.getLineSeparatorsOccurences(nextNode.getText());
                if (countBreakLineAfterHeader == 0) {
                    countBreakLine = 2;
                } else if (countBreakLineAfterHeader == 1) {
                    countBreakLine = 1;
                }
            } else {
                countBreakLine = 2;
            }
        }
        
        return countBreakLine;
    }
    
    private int computeBreakLineBeforeImport(PsiElement element) {
        int countBreakLine = 0;
        if (element instanceof JetPackageDirective) {
            if (!element.getText().isEmpty()) {
                countBreakLine = 2;
            }
        } else {
            countBreakLine = 1;
        }
        
        return countBreakLine;
    }
    
    private PsiElement findNodeToNewImport(IFile file) {
        JetFile jetFile = KotlinPsiManager.INSTANCE.getParsedFile(file);
        List<JetImportDirective> jetImportDirective = jetFile.getImportDirectives();
        if (jetImportDirective != null && !jetImportDirective.isEmpty()) {
            return jetImportDirective.get(jetImportDirective.size() - 1);
        }
        
        return jetFile.getPackageDirective();
    }
}
