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

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.text.JavaPartitionScanner;
import org.eclipse.jdt.ui.text.IJavaPartitions;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.DefaultTextHover;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.information.IInformationPresenter;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.information.IInformationProviderExtension;
import org.eclipse.jface.text.information.InformationPresenter;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.quickassist.QuickAssistAssistant;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.DefaultAnnotationHover;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.ui.editors.codeassist.CompletionProcessor;
import org.jetbrains.kotlin.ui.editors.outline.KotlinOutlinePopup;
import org.jetbrains.kotlin.utils.EditorUtil;

public class Configuration extends JavaSourceViewerConfiguration {
    private DoubleClickStrategy doubleClickStrategy;
    private Scanner scanner;
    private final ColorManager colorManager;
    private final JavaEditor editor;

    public Configuration(@NotNull ColorManager colorManager, @NotNull JavaEditor editor, IPreferenceStore preferenceStore) {
        super(colorManager, preferenceStore, editor, IJavaPartitions.JAVA_PARTITIONING);
        this.colorManager = colorManager;
        this.editor = editor;
    }
    
    @Override
    public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
        return new String[] {
            IDocument.DEFAULT_CONTENT_TYPE,
            IJavaPartitions.JAVA_DOC,
            IJavaPartitions.JAVA_MULTI_LINE_COMMENT,
            IJavaPartitions.JAVA_SINGLE_LINE_COMMENT,
            IJavaPartitions.JAVA_STRING,
            IJavaPartitions.JAVA_CHARACTER
        };
    }

    @Override
    public IInformationPresenter getOutlinePresenter(ISourceViewer sourceViewer, boolean doCodeResolve) {
        InformationPresenter presenter = new InformationPresenter(new IInformationControlCreator() {
            @Override
            public IInformationControl createInformationControl(Shell parent) {
                int shellStyle= SWT.RESIZE;
                int treeStyle= SWT.V_SCROLL | SWT.H_SCROLL;
                return new KotlinOutlinePopup(editor, parent, shellStyle, treeStyle);
            }
        });
        
        presenter.setInformationProvider(new OutlineInformationProvider(editor), IDocument.DEFAULT_CONTENT_TYPE);
        presenter.setSizeConstraints(45, 15, true, false);
        
        return presenter;
    }
    
    @Override
    public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
        return new DefaultTextHover(sourceViewer);
    }
    
    @Override
    public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
        return new DefaultAnnotationHover();
    }
    
    @Override
    public IQuickAssistAssistant getQuickAssistAssistant(ISourceViewer sourceViewer) {
        IQuickAssistAssistant quickAssist = new QuickAssistAssistant();
        quickAssist.setQuickAssistProcessor(new KotlinCorrectionProcessor(editor));
        quickAssist.setInformationControlCreator(getInformationControlCreator(sourceViewer));
        return quickAssist; 
    }
    
    @Override
    public IReconciler getReconciler(ISourceViewer sourceViewer) {
        KotlinReconcilingStrategy ktReconcilingStrategy = new KotlinReconcilingStrategy(editor);
        return new MonoReconciler(ktReconcilingStrategy, false);
    }

    @Override
    public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType) {
        if (doubleClickStrategy == null)
            doubleClickStrategy = new DoubleClickStrategy();
        return doubleClickStrategy;
    }

    protected Scanner getScanner() {
        if (scanner == null) {
            scanner = new Scanner(colorManager);
            scanner.setDefaultReturnToken(new Token(new TextAttribute(colorManager.getColor(IColorConstants.DEFAULT))));
        }
        return scanner;
    }
    
    @Override
    public String[] getDefaultPrefixes(ISourceViewer sourceViewer, String contentType) {
        return new String[] { "//", "" };
    }
    
    @Override
    public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType) {
        return new IAutoEditStrategy[] { new KotlinAutoIndentStrategy(editor) };
    }
    
    @Override
    public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
        return JavaPartitionScanner.JAVA_PARTITIONING;
    }
    
    @Override
    public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
        PresentationReconciler reconciler = (PresentationReconciler) super.getPresentationReconciler(sourceViewer);
        
        DefaultDamagerRepairer dr = new KotlinDefaultDamagerRepairer(getScanner(), editor);
        reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
        reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);
        
        reconciler.setDamager(dr, IJavaPartitions.JAVA_STRING);
        reconciler.setRepairer(dr, IJavaPartitions.JAVA_STRING);
        
        NonRuleBasedDamagerRepairer ndr = new NonRuleBasedDamagerRepairer(new TextAttribute(
                colorManager.getColor(IColorConstants.COMMENT)));
        reconciler.setDamager(ndr, IJavaPartitions.JAVA_MULTI_LINE_COMMENT);
        reconciler.setRepairer(ndr, IJavaPartitions.JAVA_MULTI_LINE_COMMENT);
        
        reconciler.setDamager(ndr, IJavaPartitions.JAVA_SINGLE_LINE_COMMENT);
        reconciler.setRepairer(ndr, IJavaPartitions.JAVA_SINGLE_LINE_COMMENT);
        
        reconciler.setDamager(ndr, IJavaPartitions.JAVA_DOC);
        reconciler.setRepairer(ndr, IJavaPartitions.JAVA_DOC);
        
        return reconciler;
    }

    @Override
    public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
         ContentAssistant assistant= new ContentAssistant();
         CompletionProcessor completionProcessor = new CompletionProcessor(editor);
         
         assistant.setContentAssistProcessor(completionProcessor, IDocument.DEFAULT_CONTENT_TYPE);
         assistant.addCompletionListener(completionProcessor);
         assistant.enableAutoActivation(true);
         assistant.setAutoActivationDelay(500);
         assistant.setProposalPopupOrientation(IContentAssistant.PROPOSAL_OVERLAY);
         assistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);

        return assistant;
    }
    
    private static class OutlineInformationProvider implements IInformationProvider, IInformationProviderExtension {
        
        private final AbstractTextEditor editor;
        
        public OutlineInformationProvider(AbstractTextEditor editor) {
            this.editor = editor;
        }
        
        @Override
        public IRegion getSubject(ITextViewer textViewer, int offset) {
            return new Region(offset, 0);
        }
        
        @Override
        public String getInformation(ITextViewer textViewer, IRegion subject) {
            return getInformation2(textViewer, subject).toString();
        }
        
        @Override
        public Object getInformation2(ITextViewer textViewer, IRegion subject) {
            return KotlinPsiManager.INSTANCE.getParsedFile(EditorUtil.getFile(editor));
        }
    }
}