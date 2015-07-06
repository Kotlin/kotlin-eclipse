package org.jetbrains.kotlin.ui.editors;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jdt.ui.actions.SelectionDispatchAction;
import org.eclipse.jface.text.ITextSelection;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

public class KotlinSelectEnclosingAction extends SelectionDispatchAction {
    
    private static final String ACTION_DESCRIPTION = "Select enclosing element";

    public static final String SELECT_ENCLOSING_TEXT = "SelectEnclosing";
    
    private final KotlinEditor editor;
    
    public KotlinSelectEnclosingAction(KotlinEditor editor) {
        super(editor.getSite());
        this.editor = editor;
        setText(ACTION_DESCRIPTION);
        setActionDefinitionId(IJavaEditorActionDefinitionIds.SELECT_ENCLOSING);
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
            PsiElement el = parsedCode.findElementAt(selection.getOffset());
            if (el == null) {
                return;
            }
            while (!isSelectionStrictlyWithin(selection, el)) {
                el = el.getParent();
            }
            TextRange elementRange = el.getTextRange();
            editor.selectAndReveal(elementRange.getStartOffset(), elementRange.getLength());
        } else {
            KotlinLogger.logError("Failed to retrieve IFile from editor " + editor, null);
        }
    }
    
    private boolean isSelectionStrictlyWithin(ITextSelection selection, PsiElement element) {
        TextRange elementRange = element.getTextRange();
        int selectionStartOffset = selection.getOffset();
        int selectionEndOffset = selectionStartOffset + selection.getLength();
        int elementStartOffset = elementRange.getStartOffset();
        int elementEndOffset = elementRange.getEndOffset();
        //selection is within element
        if (elementStartOffset <= selectionStartOffset && selectionEndOffset <= elementEndOffset) {
          //check strictness
            return elementStartOffset < selectionStartOffset || selectionEndOffset < elementEndOffset; 
        }
        return false;
    }
    
}
