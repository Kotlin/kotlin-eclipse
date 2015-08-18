package org.jetbrains.kotlin.ui.editors.selection;

import org.eclipse.jdt.core.SourceRange;
import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.SelectionHistory;
import org.eclipse.jdt.ui.actions.SelectionDispatchAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.ui.editors.KotlinEditor;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;

abstract public class KotlinSemanticSelectionAction extends SelectionDispatchAction {
    
    public final static String HISTORY = "RestoreLastSelection";
    
    protected KotlinEditor editor;
    protected SelectionHistory history;
    
    protected KotlinSemanticSelectionAction(KotlinEditor editor, SelectionHistory history) {
        super(editor.getJavaEditor().getSite());
        this.editor = editor;
        this.history = history;
    }
    
    protected enum ElementSelection {
        NotSelected, PartiallySelected, StrictlyWithinSelection, IsEqualToSelection, IsEnclosingSelection
    }
    
    @Override
    public void run(ITextSelection selection) {
        IDocument document = editor.getDocument();
        JetFile jetFile = editor.getParsedFile();
        
        if (jetFile == null) {
            return;
        }
        
        TextRange crRange = new TextRange(selection.getOffset(), selection.getOffset() + selection.getLength());
        TextRange selectedRange = LineEndUtil.lfRangeFromCrRange(crRange, document);
        PsiElement enclosingElement = getEnclosingElementForSelection(jetFile, selectedRange);
        if (enclosingElement == null) {
            return;
        }
        TextRange elementRange = runInternalSelection(enclosingElement, selectedRange, selection.getText());
        history.remember(new SourceRange(selection.getOffset(), selection.getLength()));
        try {
            history.ignoreSelectionChanges();
            
            TextRange convertedRange = LineEndUtil.crRangeFromLfRange(jetFile.getText(), elementRange, document);
            editor.getJavaEditor().selectAndReveal(convertedRange.getStartOffset(), convertedRange.getLength());
        } finally {
            history.listenToSelectionChanges();
        }
    }
    
    @Nullable
    protected PsiElement getEnclosingElementForSelection(PsiFile parsedCode, TextRange selectedRange) {
        PsiElement selectedElement = parsedCode.findElementAt(selectedRange.getStartOffset());
        if (selectedElement instanceof PsiWhiteSpace) {
            PsiElement shiftedElement = parsedCode.findElementAt(selectedRange.getStartOffset() - 1);
            if (!(shiftedElement instanceof PsiWhiteSpace)) {
                selectedElement = shiftedElement;
            }
        }
        while (selectedElement != null
                && checkSelection(selectedElement, selectedRange) != ElementSelection.IsEnclosingSelection) {
            selectedElement = selectedElement.getParent();
        }
        return selectedElement;
    }
    
    @NotNull
    abstract protected TextRange runInternalSelection(PsiElement enclosingElement, TextRange selectedRange,
            String selectedText);
    
    protected ElementSelection checkSelection(PsiElement element, TextRange selectedRange) {
        TextRange elementRange = element.getTextRange();
        
        int selectionStartOffset = selectedRange.getStartOffset();
        int selectionEndOffset = selectedRange.getEndOffset();
        int elementStartOffset = elementRange.getStartOffset();
        int elementEndOffset = elementRange.getEndOffset();
        
        if (selectionStartOffset == selectionEndOffset) {
            if (selectionEndOffset < elementStartOffset || elementEndOffset < selectionStartOffset) {
                return ElementSelection.NotSelected;
            }
        } else if (selectionEndOffset <= elementStartOffset || elementEndOffset <= selectionStartOffset) {
            return ElementSelection.NotSelected;
        }
        
        if (selectionStartOffset <= elementStartOffset && elementEndOffset <= selectionEndOffset) {
            if (selectionStartOffset < elementStartOffset || elementEndOffset < selectionEndOffset) {
                return ElementSelection.StrictlyWithinSelection;
            }
            
            return ElementSelection.IsEqualToSelection;
        }
        
        if (elementStartOffset <= selectionStartOffset && selectionEndOffset <= elementEndOffset) {
            return ElementSelection.IsEnclosingSelection;
        }
        
        return ElementSelection.PartiallySelected;
    }
    
    @Nullable
    protected PsiElement findSelectionCandidate(@NotNull PsiElement enclosingElement,
            PsiElementChildrenIterable elementChildren, TextRange selectedRange, String selectedText) {
        boolean isSelectionCandidate = false;
        
        // if selected text is all whitespaces then select enclosing
        if (!selectedText.isEmpty() && selectedText.trim().isEmpty()) {
            return null;
        }
        
        for (PsiElement currentChild : elementChildren) {
            ElementSelection selectionType = checkSelection(currentChild, selectedRange);
            // if all completely selected elements are not the children of the
            // enclosing element, then select enclosing element
            if (selectionType == ElementSelection.PartiallySelected && !(currentChild instanceof PsiWhiteSpace)) {
                return null;
            }
            if (selectionType == ElementSelection.NotSelected) {
                // if we're already looking for selection candidate, select if
                // not whitespace
                if (!(currentChild instanceof PsiWhiteSpace) && !currentChild.getText().isEmpty()
                        && isSelectionCandidate) {
                    return currentChild;
                }
            } else {
                // next child is selection candidate
                isSelectionCandidate = true;
            }
        }
        
        return null;
    }
}