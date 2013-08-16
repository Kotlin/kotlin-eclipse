package org.jetbrains.kotlin.ui.editors.outline;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.jetbrains.kotlin.parser.KotlinParser;
import org.jetbrains.kotlin.utils.EditorUtil;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

public class KotlinOutlinePage extends ContentOutlinePage {
    
    private final JavaEditor editor;
    private TreeViewer viewer;
    
    public KotlinOutlinePage(JavaEditor editor) {
        this.editor = editor;
    }
    
    @Override
    public void createControl(Composite parent) {
        super.createControl(parent);
        
        viewer = getTreeViewer();
        viewer.setContentProvider(new PsiContentProvider());
        viewer.setLabelProvider(new PsiLabelProvider());
        viewer.addSelectionChangedListener(this);
        
        setInputAndExpand();
    }
    
    @Override
    public void selectionChanged(SelectionChangedEvent event) {
        super.selectionChanged(event);
        ITreeSelection treeSelection = (ITreeSelection) event.getSelection();
        if (!treeSelection.isEmpty()) {
            PsiElement psiElement = (PsiElement) treeSelection.getFirstElement();
            
            int offset = EditorUtil.getOffsetInEditor(editor, psiElement.getTextOffset());
             
            editor.selectAndReveal(offset, 0);
       }
    }
    
    private void setInputAndExpand() {
        IFile file = EditorUtil.getFile(editor);
        PsiFile psiFile = KotlinParser.getPsiFile(file);
        viewer.setInput(psiFile);
        viewer.expandAll();
    }
    
    public void refresh() {
        if (viewer == null) {
            return;
        }
        
        if (!viewer.getControl().isDisposed()) {
            setInputAndExpand();
        }
    }
}
