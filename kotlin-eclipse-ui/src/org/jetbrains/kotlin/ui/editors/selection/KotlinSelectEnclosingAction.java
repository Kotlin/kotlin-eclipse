package org.jetbrains.kotlin.ui.editors.selection;

import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.SelectionHistory;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jface.text.ITextSelection;
import org.jetbrains.kotlin.ui.editors.KotlinEditor;
import org.jetbrains.kotlin.ui.editors.selection.handlers.KotlinElementSelectioner;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;

public class KotlinSelectEnclosingAction extends KotlinSemanticSelectionAction {
    
    private static final String ACTION_DESCRIPTION = "Select enclosing element";

    public static final String SELECT_ENCLOSING_TEXT = "SelectEnclosing";
    
    public KotlinSelectEnclosingAction(KotlinEditor editor, SelectionHistory history) {
        super(editor, history);
        setText(ACTION_DESCRIPTION);
        setActionDefinitionId(IJavaEditorActionDefinitionIds.SELECT_ENCLOSING);
    }

    @Override
    protected TextRange runInternalSelection(PsiElement enclosingElement, ITextSelection selection) {
        return KotlinElementSelectioner.INSTANCE$.selectEnclosing(enclosingElement, 
                getCrConvertedTextRange(selection));
    }
}
