package org.jetbrains.kotlin.ui.formatter;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jface.text.IDocument;
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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.formatter.FormatterUtil;
import com.intellij.psi.formatter.WhiteSpaceFormattingStrategy;
import com.intellij.psi.formatter.WhiteSpaceFormattingStrategyFactory;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.CompositeElement;
import com.intellij.psi.impl.source.tree.Factory;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.psi.impl.source.tree.SharedImplUtil;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.CharTable;

public class EclipseBasedFormattingModel implements FormattingModelEx {
    
    private static final Logger LOG = Logger.getInstance("#com.intellij.psi.formatter.PsiBasedFormattingModel");
    
    private final Project myProject;
    private final ASTNode myASTNode;
    private final EclipseFormattingModel myDocumentModel;
    @NotNull
    private final Block myRootBlock;
    protected boolean myCanModifyAllWhiteSpaces = false;
    private final IDocument document;
    private final List<ReplaceEdit> edits = new ArrayList<>();
    
    public EclipseBasedFormattingModel(final PsiFile file, @NotNull final Block rootBlock,
            final EclipseFormattingModel documentModel, @NotNull final IDocument document) {
        myASTNode = SourceTreeToPsiMap.psiElementToTree(file);
        myDocumentModel = documentModel;
        myRootBlock = rootBlock;
        myProject = file.getProject();
        this.document = document;
    }
    
    @Override
    public TextRange replaceWhiteSpace(TextRange textRange, String whiteSpace) {
        return replaceWhiteSpace(textRange, null, whiteSpace);
    }
    
    @Override
    public TextRange replaceWhiteSpace(TextRange textRange, @Nullable ASTNode nodeAfter, String whiteSpace) {
        String whiteSpaceToUse = myDocumentModel.adjustWhiteSpaceIfNecessary(whiteSpace, textRange.getStartOffset(),
                textRange.getEndOffset(), nodeAfter, true).toString();
        final String wsReplaced = replaceWithPSI(textRange, whiteSpaceToUse);
        
        if (wsReplaced != null) {
            return new TextRange(textRange.getStartOffset(), textRange.getStartOffset() + wsReplaced.length());
        } else {
            return textRange;
        }
    }
    
    @Override
    public TextRange shiftIndentInsideRange(ASTNode node, TextRange textRange, int shift) {
        return textRange; // TODO: Remove this method from here...
    }
    
    @Override
    public void commitChanges() {
        applyChanges();
    }
    
    @Nullable
    private String replaceWithPSI(final TextRange textRange, final String whiteSpace) {
        final int offset = textRange.getEndOffset();
        ASTNode leafElement = findElementAt(offset);
        
        if (leafElement != null) {
            if (leafElement.getPsi() instanceof PsiFile) {
                return null;
            } else {
                if (!leafElement.getPsi().isValid()) {
                    String message = "Invalid element found in '\n" + myASTNode.getText() + "\n' at " + offset + "("
                            + myASTNode.getText().substring(offset, Math.min(offset + 10, myASTNode.getTextLength()));
                    LOG.error(message);
                }
                return replaceWithPsiInLeaf(textRange, whiteSpace, leafElement);
            }
        } else if (textRange.getEndOffset() == myASTNode.getTextLength()) {
            
            CodeStyleManager.getInstance(myProject).performActionWithFormatterDisabled(new Runnable() {
                @Override
                public void run() {
                    FormatterUtil.replaceLastWhiteSpace(myASTNode, whiteSpace, textRange);
                }
            });
            
            return whiteSpace;
        } else {
            return null;
        }
    }
    
    @Nullable
    protected String replaceWithPsiInLeaf(final TextRange textRange, final String whiteSpace,
            final ASTNode leafElement) {
        if (!myCanModifyAllWhiteSpaces) {
            if (leafElement.getElementType() == TokenType.WHITE_SPACE)
                return null;
        }
        
        replaceWhiteSpace(whiteSpace, leafElement, TokenType.WHITE_SPACE, textRange);
        
        return whiteSpace;
    }
    
    private void applyChanges() {
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

    @Nullable
    protected ASTNode findElementAt(final int offset) {
        PsiFile containingFile = myASTNode.getPsi().getContainingFile();
        Project project = containingFile.getProject();
        assert !PsiDocumentManager.getInstance(project).isUncommited(myDocumentModel.getDocument());
        // TODO:default project can not be used for injections, because latter
        // might wants (unavailable) indices
        PsiElement psiElement = containingFile.findElementAt(offset);
        if (psiElement == null)
            return null;
        return psiElement.getNode();
    }
    
    @Override
    @NotNull
    public FormattingDocumentModel getDocumentModel() {
        return myDocumentModel;
    }
    
    @Override
    @NotNull
    public Block getRootBlock() {
        return myRootBlock;
    }
    
    public void canModifyAllWhiteSpaces() {
        myCanModifyAllWhiteSpaces = true;
    }
    
    public void replaceWhiteSpace(final String whiteSpace, final ASTNode leafElement,
            final IElementType whiteSpaceToken, @Nullable final TextRange textRange) {
        final CharTable charTable = SharedImplUtil.findCharTableByTree(leafElement);
        
        ASTNode treePrev = findPreviousWhiteSpace(leafElement, whiteSpaceToken);
        if (treePrev == null) {
            treePrev = getWsCandidate(leafElement);
        }
        
        if (treePrev != null && treePrev.getText().trim().isEmpty() && treePrev.getElementType() != whiteSpaceToken
                && treePrev.getTextLength() > 0 && !whiteSpace.isEmpty()) {
            LeafElement whiteSpaceElement = Factory.createSingleLeafElement(treePrev.getElementType(), whiteSpace,
                    charTable, SharedImplUtil.getManagerByTree(leafElement));
            
            ASTNode treeParent = treePrev.getTreeParent();
            treeParent.replaceChild(treePrev, whiteSpaceElement);
        } else {
            LeafElement whiteSpaceElement = Factory.createSingleLeafElement(whiteSpaceToken, whiteSpace, charTable,
                    SharedImplUtil.getManagerByTree(leafElement));
            
            if (treePrev == null) {
                if (!whiteSpace.isEmpty()) {
                    addWhiteSpace(leafElement, whiteSpaceElement);
                }
            } else {
                if (!(treePrev.getElementType() == whiteSpaceToken)) {
                    if (!whiteSpace.isEmpty()) {
                        addWhiteSpace(treePrev, whiteSpaceElement);
                    }
                } else {
                    if (treePrev.getElementType() == whiteSpaceToken) {
                        final CompositeElement treeParent = (CompositeElement) treePrev.getTreeParent();
                        if (!whiteSpace.isEmpty()) {
                            // LOG.assertTrue(textRange == null ||
                            // treeParent.getTextRange().equals(textRange));
//                            treeParent.replaceChild(treePrev, whiteSpaceElement);
                            replaceChild(treePrev, whiteSpaceElement);
                        } else {
                            removeChild(treePrev);
                        }
                        
                        // There is a possible case that more than one PSI
                        // element is matched by the target text range.
                        // That is the case, for example, for Python's
                        // multi-line expression. It may looks like below:
                        // import contextlib,\
                        // math, decimal
                        // Here single range contains two blocks: '\' & '\n '.
                        // So, we may want to replace that range to another
                        // text, hence,
                        // we replace last element located there with it ('\n ')
                        // and want to remove any remaining elements ('\').
                        ASTNode removeCandidate = findPreviousWhiteSpace(whiteSpaceElement, whiteSpaceToken);
                        while (textRange != null && removeCandidate != null
                                && removeCandidate.getStartOffset() >= textRange.getStartOffset()) {
                            treePrev = findPreviousWhiteSpace(removeCandidate, whiteSpaceToken);
                            removeCandidate.getTreeParent().removeChild(removeCandidate);
                            removeCandidate = treePrev;
                        }
                        // treeParent.subtreeChanged();
                    }
                }
            }
        }
    }
    
    @Nullable
    private static ASTNode findPreviousWhiteSpace(final ASTNode leafElement, final IElementType whiteSpaceTokenType) {
        final int offset = leafElement.getTextRange().getStartOffset() - 1;
        if (offset < 0)
            return null;
        final PsiElement psiElement = SourceTreeToPsiMap.treeElementToPsi(leafElement);
        if (psiElement == null) {
            return null;
        }
        final PsiElement found = psiElement.getContainingFile().findElementAt(offset);
        if (found == null)
            return null;
        final ASTNode treeElement = found.getNode();
        if (treeElement != null && treeElement.getElementType() == whiteSpaceTokenType)
            return treeElement;
        return null;
    }
    
    @Nullable
    private static ASTNode getWsCandidate(@Nullable ASTNode node) {
        if (node == null)
            return null;
        ASTNode treePrev = node.getTreePrev();
        if (treePrev != null) {
            if (treePrev.getElementType() == TokenType.WHITE_SPACE) {
                return treePrev;
            } else if (treePrev.getTextLength() == 0) {
                return getWsCandidate(treePrev);
            } else {
                return node;
            }
        }
        final ASTNode treeParent = node.getTreeParent();
        
        if (treeParent == null || treeParent.getTreeParent() == null) {
            return node;
        } else {
            return getWsCandidate(treeParent);
        }
    }
    
    private void addWhiteSpace(final ASTNode treePrev, final LeafElement whiteSpaceElement) {
        for (WhiteSpaceFormattingStrategy strategy : WhiteSpaceFormattingStrategyFactory.getAllStrategies()) {
            if (strategy.addWhitespace(treePrev, whiteSpaceElement)) {
                return;
            }
        }
        
        ReplaceEdit edit = new ReplaceEdit(convertOffset(treePrev.getStartOffset()), 0, whiteSpaceElement.getText());
        edits.add(edit);
    }
    
    private void removeChild(@NotNull ASTNode child) {
        ReplaceEdit edit = new ReplaceEdit(convertOffset(child.getStartOffset()), child.getTextLength(), "");
        edits.add(edit);
    }
    
    private void replaceChild(@NotNull ASTNode oldChild, @NotNull ASTNode newChild) {
        ReplaceEdit edit = new ReplaceEdit(convertOffset(oldChild.getStartOffset()), oldChild.getTextLength(), newChild.getText());
        edits.add(edit);
    }
    
    private int convertOffset(int offset) {
        return LineEndUtil.convertLfToDocumentOffset(myASTNode.getPsi().getContainingFile().getText(), offset, document);
    }
}
