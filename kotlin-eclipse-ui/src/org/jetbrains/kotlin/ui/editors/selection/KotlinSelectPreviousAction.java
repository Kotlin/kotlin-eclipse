package org.jetbrains.kotlin.ui.editors.selection;

import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.SelectionHistory;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.ui.editors.KotlinEditor;
import org.jetbrains.kotlin.ui.editors.selection.handlers.KotlinElementSelectioner;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;

public class KotlinSelectPreviousAction extends KotlinSemanticSelectionAction {
    
    private static final String ACTION_DESCRIPTION = "Select previous element";
    
    public static final String SELECT_PREVIOUS_TEXT = "SelectPrevious";
    
    public KotlinSelectPreviousAction(KotlinEditor editor, SelectionHistory history) {
        super(editor, history);
        setText(ACTION_DESCRIPTION);
        setActionDefinitionId(IJavaEditorActionDefinitionIds.SELECT_PREVIOUS);
    }
    
    @Override
    protected @NotNull TextRange runInternalSelection(PsiElement enclosingElement, TextRange selectedRange,
            String selectedText) {
        PsiElement selectionCandidate = findSelectionCandidate(enclosingElement,
                PsiElementChildrenIterable.backwardChildrenIterator(enclosingElement), selectedRange, selectedText);
        if (selectionCandidate == null) {
            return KotlinElementSelectioner.INSTANCE.selectEnclosing(enclosingElement, selectedRange);
        }
        return KotlinElementSelectioner.INSTANCE.selectPrevious(enclosingElement, selectionCandidate, selectedRange);
    }
}
