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
package org.jetbrains.kotlin.ui.editors;

import org.eclipse.core.resources.IFile;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.SelectionHistory;
import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.StructureSelectHistoryAction;
import org.eclipse.jdt.internal.ui.text.JavaColorManager;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jdt.ui.text.IColorManager;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.model.KotlinEnvironment;
import org.jetbrains.kotlin.eclipse.ui.utils.IndenterUtil;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.psi.JetPsiFactory;
import org.jetbrains.kotlin.ui.debug.KotlinToggleBreakpointAdapter;
import org.jetbrains.kotlin.ui.editors.outline.KotlinOutlinePage;
import org.jetbrains.kotlin.ui.editors.selection.KotlinSelectEnclosingAction;
import org.jetbrains.kotlin.ui.editors.selection.KotlinSelectNextAction;
import org.jetbrains.kotlin.ui.editors.selection.KotlinSelectPreviousAction;
import org.jetbrains.kotlin.ui.editors.selection.KotlinSemanticSelectionAction;
import org.jetbrains.kotlin.ui.navigation.KotlinOpenEditor;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;

public class KotlinFileEditor extends CompilationUnitEditor implements KotlinEditor {
    private final IColorManager colorManager;
    private final BracketInserter bracketInserter;
    private KotlinOutlinePage kotlinOutlinePage = null;
    private KotlinToggleBreakpointAdapter kotlinToggleBreakpointAdapter = null;
    private IJavaProject javaProject = null;
    
    public KotlinFileEditor() {
        super();
        colorManager = new JavaColorManager();
        bracketInserter = new BracketInserter();
    }
    
    @Override
    public Object getAdapter(@SuppressWarnings("rawtypes") Class required) {
        if (IContentOutlinePage.class.equals(required)) {
            return getKotlinOutlinePage();
        } else if (IToggleBreakpointsTarget.class.equals(required)) {
            return getKotlinToggleBreakpointAdapter();
        }
        
        return super.getAdapter(required);
    }
    
    @Override
    public void createPartControl(Composite parent) {
        setSourceViewerConfiguration(new FileEditorConfiguration(colorManager, this, getPreferenceStore()));
        
        super.createPartControl(parent);
        ISourceViewer sourceViewer = getSourceViewer();
        if (sourceViewer instanceof ITextViewerExtension) {
            bracketInserter.setSourceViewer(sourceViewer);
            bracketInserter.addBrackets('{', '}');
            ((ITextViewerExtension) sourceViewer).prependVerifyKeyListener(bracketInserter);
        }
    }
    
    @Override
    protected boolean isMarkingOccurrences() {
        return false;
    }
    
    @Override
    protected boolean isTabsToSpacesConversionEnabled() {
        return IndenterUtil.isSpacesForTabs();
    }
    
    @Override
    protected void createActions() {
        super.createActions();
        
        setAction("QuickFormat", null);
        
        IAction formatAction = new KotlinFormatAction(this);
        setAction(KotlinFormatAction.FORMAT_ACTION_TEXT, formatAction);
        markAsStateDependentAction(KotlinFormatAction.FORMAT_ACTION_TEXT, true);
        markAsSelectionDependentAction(KotlinFormatAction.FORMAT_ACTION_TEXT, true);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(formatAction, IJavaHelpContextIds.FORMAT_ACTION);
        
        SelectionHistory selectionHistory = new SelectionHistory(this);
        
        StructureSelectHistoryAction historyAction = new StructureSelectHistoryAction(this, selectionHistory);
        historyAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SELECT_LAST);
        setAction(KotlinSemanticSelectionAction.HISTORY, historyAction);
        selectionHistory.setHistoryAction(historyAction);
        
        setAction(KotlinOpenDeclarationAction.OPEN_EDITOR_TEXT, new KotlinOpenDeclarationAction(this));
        
        setAction(KotlinSelectEnclosingAction.SELECT_ENCLOSING_TEXT, new KotlinSelectEnclosingAction(this,
                selectionHistory));
        setAction(KotlinSelectPreviousAction.SELECT_PREVIOUS_TEXT, new KotlinSelectPreviousAction(this,
                selectionHistory));
        setAction(KotlinSelectNextAction.SELECT_NEXT_TEXT, new KotlinSelectNextAction(this, selectionHistory));
        
    }
    
    @Override
    public void dispose() {
        colorManager.dispose();
        ISourceViewer sourceViewer = getSourceViewer();
        if (sourceViewer instanceof ITextViewerExtension) {
            ((ITextViewerExtension) sourceViewer).removeVerifyKeyListener(bracketInserter);
        }
        
        super.dispose();
    }
    
    @Override
    public void setSelection(IJavaElement element) {
        KotlinOpenEditor.revealKotlinElement(this, element);
    }
    
    @Override
    protected void initializeKeyBindingScopes() {
        setKeyBindingScopes(new String[] { 
                "org.jetbrains.kotlin.eclipse.ui.kotlinEditorScope",
                "org.eclipse.jdt.ui.javaEditorScope" });
    }
    
    @NotNull
    private KotlinOutlinePage getKotlinOutlinePage() {
        if (kotlinOutlinePage == null) {
            kotlinOutlinePage = new KotlinOutlinePage(this);
        }
        
        return kotlinOutlinePage;
    }
    
    @NotNull
    private KotlinToggleBreakpointAdapter getKotlinToggleBreakpointAdapter() {
        if (kotlinToggleBreakpointAdapter == null) {
            kotlinToggleBreakpointAdapter = new KotlinToggleBreakpointAdapter();
        }
        
        return kotlinToggleBreakpointAdapter;
    }

    @Override
    @NotNull
    public JavaEditor getJavaEditor() {
        return this;
    }
    
    @Nullable
    public IFile getFile() {
        return (IFile) getEditorInput().getAdapter(IFile.class); 
    }

    @Override
    @Nullable
    public JetFile getParsedFile() {
        IFile file = getFile();
        if (file == null) {
            IJavaProject javaProject = getJavaProject();
            if (javaProject == null) {
                return null;
            }
            KotlinEnvironment environment = KotlinEnvironment.getEnvironment(javaProject);
            Project ideaProject = environment.getProject();
            return new JetPsiFactory(ideaProject).createFile(StringUtil.convertLineSeparators(getDocument().get()));
        }
        return KotlinPsiManager.INSTANCE.getParsedFile(file);
    }

    @Override
    @Nullable
    public synchronized IJavaProject getJavaProject() {
        if (javaProject == null) {
            IFile file = getFile();
            if (file == null) {
                return null;
            }
            javaProject = JavaCore.create(file.getProject());
        }
        return javaProject;
    }

    @Override
    public boolean isEditable() {
        return getFile() != null;
    }
    
    @Override
    @NotNull
    public IDocument getDocument() {
        return getDocumentProvider().getDocument(getEditorInput());
    }
}
