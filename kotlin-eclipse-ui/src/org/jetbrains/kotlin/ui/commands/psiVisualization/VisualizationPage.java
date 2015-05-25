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
package org.jetbrains.kotlin.ui.commands.psiVisualization;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil;
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;

public final class VisualizationPage extends Dialog {

    private final Point pageSize = new Point(750, 650);
    private final String sourceCode;
    private final IFile file;
    
    private final String title = "Psi Viewer";
    
    public VisualizationPage(Shell parentShell, String sourceCode, IFile file) {
        super(parentShell);
        
        if (sourceCode == null || file == null || !file.exists()) {
            throw new IllegalArgumentException();
        }
        
        this.sourceCode = sourceCode;
        this.file = file;
    }
    
    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        
        createControls(composite);
        
        return composite;
    }
    
    private void createControls(Composite composite) {
        setDescriptionLabel(composite);
        Text programText = setTextProgram(composite);
        setTreeViewer(composite, programText);
    }
    
    private void setTreeViewer(Composite composite, final Text programText) {
        TreeViewer psiTreeViewer = new TreeViewer(composite);
        psiTreeViewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
        psiTreeViewer.setContentProvider(new PsiContentProvider());
        psiTreeViewer.setLabelProvider(new LabelProvider());
        
        ASTNode parsedAst = KotlinPsiManager.INSTANCE.getParsedFile(file).getNode();
        psiTreeViewer.setInput(parsedAst);
        
        final String parsedText = parsedAst.getText();
        
        final IDocument document = EditorUtil.getDocument(file);
        
        psiTreeViewer.addDoubleClickListener(new IDoubleClickListener() {            
            @Override
            public void doubleClick(DoubleClickEvent event) {
                IStructuredSelection thisSelection = (IStructuredSelection) event.getSelection();
                ASTNode selectedNode = (ASTNode) thisSelection.getFirstElement();
                TextRange selectedNodeRange = selectedNode.getTextRange();
                
                int start = LineEndUtil.convertLfToDocumentOffset(parsedText, selectedNodeRange.getStartOffset(), document);
                int end = LineEndUtil.convertLfToDocumentOffset(parsedText, selectedNodeRange.getEndOffset(), document);
                
                programText.setSelection(start, end);
                programText.showSelection();
            }
        });
    }
    
    private void setDescriptionLabel(Composite composite) {
        Label descriptionLabel = new Label(composite, SWT.LEFT | SWT.FILL);
        descriptionLabel.setText("Shows PSI structure for Kotlin file: " + file.getName());
        
        Label separator = new Label(composite, SWT.HORIZONTAL | SWT.SEPARATOR);
        GridData sgd = new GridData(GridData.FILL_HORIZONTAL);
        separator.setLayoutData(sgd);
        
    }
    
    private Text setTextProgram(Composite composite) {
        final Text programText = new Text(composite, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        programText.setEditable(false);
        programText.setText(sourceCode);
        GridData pgd = new GridData(GridData.FILL_HORIZONTAL);
        pgd.heightHint = pageSize.y / 3;
        programText.setLayoutData(pgd);
        
        return programText;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        Button closeButton = createButton(parent, IDialogConstants.CLOSE_ID, IDialogConstants.CLOSE_LABEL, false);
        closeButton.addSelectionListener(new SelectionAdapter() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                setReturnCode(OK);
                close();
            }
        });
    }
    
    @Override
    protected Point getInitialSize() {
        return pageSize;
    }
    
    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(title);
    }
}