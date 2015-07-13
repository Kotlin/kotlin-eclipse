package org.jetbrains.kotlin.ui.editors.selection;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.SourceRange;
import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.SelectionHistory;
import org.eclipse.jdt.ui.actions.SelectionDispatchAction;
import org.eclipse.jface.text.ITextSelection;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil;
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
        super(editor.getSite());
        this.editor = editor;
        this.history = history;
    }
    
    protected enum ElementSelection {
        NotSelected,
        PartiallySelected,
        StrictlyWithinSelection,
        IsEqualToSelection,
        IsEnclosingSelection
    }
    
    @Override
    public void run(ITextSelection selection) {
        String sourceCode = EditorUtil.getSourceCode(editor);
        IFile file = EditorUtil.getFile(editor);
        if (file != null) {
            PsiFile parsedCode = KotlinPsiManager.getKotlinFileIfExist(file, sourceCode);
            if (parsedCode == null) {
                return;
            }
            TextRange selectedRange = getCrConvertedTextRange(selection);
            PsiElement enclosingElement = getEnclosingElementForSelection(parsedCode, selectedRange);
            if (enclosingElement == null) {
                return;
            }
            TextRange elementRange = runInternalSelection(enclosingElement, selection);
            history.remember(new SourceRange(selection.getOffset(), selection.getLength()));
            try {
                history.ignoreSelectionChanges();
                TextRange convertedRange = getLfConvertedTextRange(elementRange);
                editor.selectAndReveal(convertedRange.getStartOffset(), convertedRange.getLength());
            } finally {
                history.listenToSelectionChanges();
            }
        } else {
            KotlinLogger.logError("Failed to retrieve IFile from editor " + editor, null);
        }
    }
    
    protected TextRange getCrConvertedTextRange(ITextSelection selection) {
        int startOffset = LineEndUtil.convertCrToDocumentOffset(EditorUtil.getDocument(editor),
                selection.getOffset());
        int endOffset =  LineEndUtil.convertCrToDocumentOffset(EditorUtil.getDocument(editor),
                selection.getOffset()+selection.getLength());
        return new TextRange(startOffset, endOffset);
    }
    
    protected TextRange getLfConvertedTextRange(TextRange range) {
        IFile file = EditorUtil.getFile(editor);
        if (file!=null) {
            JetFile jetFile = KotlinPsiManager.INSTANCE.getParsedFile(file);

            int startOffset = LineEndUtil.convertLfToDocumentOffset(jetFile.getText(),
                    range.getStartOffset(), EditorUtil.getDocument(editor));
            int endOffset =  LineEndUtil.convertLfToDocumentOffset(jetFile.getText(),
                    range.getEndOffset(), EditorUtil.getDocument(editor));
            return new TextRange(startOffset, endOffset);
        } else {
            KotlinLogger.logError("Failed to retrieve IFile from editor " + editor, null);
            return null;
        }
    }
    
    protected PsiElement getEnclosingElementForSelection(PsiFile parsedCode, TextRange selectedRange) {
        PsiElement selectedElement = parsedCode.findElementAt(selectedRange.getStartOffset());
        if (selectedElement instanceof PsiWhiteSpace) {
            PsiElement shiftedElement = parsedCode.findElementAt(selectedRange.getStartOffset()-1);
            if (!(shiftedElement instanceof PsiWhiteSpace)) {
                selectedElement = shiftedElement;
            }
        }
        while (selectedElement!=null && checkSelection(selectedElement, selectedRange)!=ElementSelection.IsEnclosingSelection) {
            selectedElement = selectedElement.getParent();
        }
        return selectedElement;
    }
    
    abstract protected TextRange runInternalSelection(PsiElement enclosingElement, ITextSelection selection);
    
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
            if (selectionStartOffset < elementStartOffset  || elementEndOffset < selectionEndOffset) {
                return ElementSelection.StrictlyWithinSelection;
            }
            return ElementSelection.IsEqualToSelection;
        }
        if (elementStartOffset <= selectionStartOffset && selectionEndOffset <= elementEndOffset) {
            return ElementSelection.IsEnclosingSelection;
        }
        return ElementSelection.PartiallySelected;
    }
}
