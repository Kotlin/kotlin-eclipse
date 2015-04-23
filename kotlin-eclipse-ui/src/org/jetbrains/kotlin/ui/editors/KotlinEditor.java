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

import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.text.JavaColorManager;
import org.eclipse.jdt.ui.text.IColorManager;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.ui.debug.KotlinToggleBreakpointAdapter;
import org.jetbrains.kotlin.ui.editors.outline.KotlinOutlinePage;
import org.jetbrains.kotlin.ui.navigation.KotlinOpenEditor;

public class KotlinEditor extends CompilationUnitEditor {
    
    private final IColorManager colorManager;
    private final BracketInserter bracketInserter;
    private KotlinOutlinePage kotlinOutlinePage = null;
    private KotlinToggleBreakpointAdapter kotlinToggleBreakpointAdapter = null;
    
    public KotlinEditor() {
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
        setSourceViewerConfiguration(new Configuration(colorManager, this, getPreferenceStore()));
        
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
    protected void createActions() {
        super.createActions();
        
        setAction("QuickFormat", null);
        
        IAction formatAction = new KotlinFormatAction(this);
        setAction(KotlinFormatAction.FORMAT_ACTION_TEXT, formatAction);
        markAsStateDependentAction(KotlinFormatAction.FORMAT_ACTION_TEXT, true);
        markAsSelectionDependentAction(KotlinFormatAction.FORMAT_ACTION_TEXT, true);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(formatAction, IJavaHelpContextIds.FORMAT_ACTION);
        
        setAction(KotlinOpenDeclarationAction.OPEN_EDITOR_TEXT, new KotlinOpenDeclarationAction(this));
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
}
