package org.jetbrains.kotlin.ui.editors.selection;

import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.SelectionHistory;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.ui.editors.KotlinEditor;
import org.jetbrains.kotlin.ui.editors.selection.handlers.KotlinElementSelectioner;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;

public class KotlinSelectNextAction extends KotlinSemanticSelectionAction {
    
    private static final String ACTION_DESCRIPTION = "Select next element";
    
    public static final String SELECT_NEXT_TEXT = "SelectNext";
    
    public KotlinSelectNextAction(KotlinEditor editor, SelectionHistory history) {
        super(editor, history);
        setText(ACTION_DESCRIPTION);
        setActionDefinitionId(IJavaEditorActionDefinitionIds.SELECT_NEXT);
    }
    
    @Override
    protected @NotNull TextRange runInternalSelection(PsiElement enclosingElement, TextRange selectedRange,
            String selectedText) {
        PsiElement selectionCandidate = findSelectionCandidate(enclosingElement,
                PsiElementChildrenIterable.forwardChildrenIterator(enclosingElement), selectedRange, selectedText);
        KotlinElementSelectioner elementSelectioner = KotlinElementSelectioner.INSTANCE$;
        
        if (selectionCandidate == null) {
            return elementSelectioner.selectEnclosing(enclosingElement, selectedRange);
        }
        return elementSelectioner.selectNext(enclosingElement, selectionCandidate, selectedRange);
    }
}
