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

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.jetbrains.kotlin.ui.editors.outline.KotlinOutlinePage;

public class KotlinEditor extends CompilationUnitEditor {

    private final ColorManager colorManager;
    private final BracketInserter bracketInserter;
    private KotlinOutlinePage kotlinOutlinePage = null;
    
    public KotlinEditor() {
        super();
        colorManager = new ColorManager();
        bracketInserter = new BracketInserter();
    }
    
    @Override
    public Object getAdapter(@SuppressWarnings("rawtypes") Class required) {
        if (IContentOutlinePage.class.equals(required)) {
            if (kotlinOutlinePage == null) {
                kotlinOutlinePage = new KotlinOutlinePage(this);
            }
            return kotlinOutlinePage;
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
        formatAction.setText("Format");
        formatAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.FORMAT);
        setAction("Format", formatAction);
        markAsStateDependentAction("Format", true);
        markAsSelectionDependentAction("Format", true);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(formatAction, IJavaHelpContextIds.FORMAT_ACTION);
        
        IAction openDeclarationAction = new OpenDeclarationAction(this);
        openDeclarationAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_EDITOR);
        openDeclarationAction.setText(ActionMessages.OpenAction_declaration_label);
        setAction("OpenEditor", openDeclarationAction);
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
}
