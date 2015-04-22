/*******************************************************************************
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *******************************************************************************/
package org.jetbrains.kotlin.ui.editors.outline;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil;

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
        PsiFile psiFile = null;
        IFile file = EditorUtil.getFile(editor);
        if (file != null) {
            psiFile = KotlinPsiManager.INSTANCE.getParsedFile(file);
        } else {
            KotlinLogger.logError("Failed to retrieve IFile from editor " + editor, null);
        }
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
