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

import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.internal.ui.text.JavaPartitionScanner;
import org.eclipse.jdt.internal.ui.text.java.hover.AnnotationHover;
import org.eclipse.jdt.ui.text.IColorManager;
import org.eclipse.jdt.ui.text.IJavaPartitions;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.information.IInformationPresenter;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.information.IInformationProviderExtension;
import org.eclipse.jface.text.information.InformationPresenter;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.source.DefaultAnnotationHover;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil;
import org.jetbrains.kotlin.ui.editors.highlighting.KotlinTokenScanner;
import org.jetbrains.kotlin.ui.editors.outline.KotlinOutlinePopup;

public class Configuration extends JavaSourceViewerConfiguration {
    private KotlinTokenScanner scanner;
    private final KotlinEditor editor;

    public Configuration(@NotNull IColorManager colorManager, @NotNull KotlinEditor editor, IPreferenceStore preferenceStore) {
        super(colorManager, preferenceStore, editor.getJavaEditor(), IJavaPartitions.JAVA_PARTITIONING);
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
    public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType, int stateMask) {
        return new AnnotationHover();
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
        
        presenter.setInformationProvider(new OutlineInformationProvider(editor.getJavaEditor()), IDocument.DEFAULT_CONTENT_TYPE);
        presenter.setSizeConstraints(45, 15, true, false);
        
        return presenter;
    }
    
    @Override
    public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
        return new DefaultAnnotationHover();
    }
    
    @Override
    public IQuickAssistAssistant getQuickAssistAssistant(ISourceViewer sourceViewer) {
        return null;
    }
    
    @Override
    public IReconciler getReconciler(ISourceViewer sourceViewer) {
        return null;
    }

    @Nullable
    protected KotlinTokenScanner getScanner() {
        if (scanner == null) {
            scanner = new KotlinTokenScanner(fPreferenceStore, getColorManager());
        }

        return scanner;
    }
    
    @Override
    public String[] getDefaultPrefixes(ISourceViewer sourceViewer, String contentType) {
        return new String[] { "//", "" };
    }
    
    @Override
    public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType) {
        return null;
    }
    
    @Override
    public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
        return JavaPartitionScanner.JAVA_PARTITIONING;
    }
    
    @Override
    public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
        KotlinTokenScanner scanner = getScanner();
        return scanner != null ? getKotlinPresentaionReconciler(scanner) : null;
    }

    @Override
    public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
        return null;
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
            Object result = getInformation2(textViewer, subject);
            return result != null ? result.toString() : null;
        }
        
        @Override
        public Object getInformation2(ITextViewer textViewer, IRegion subject) {
            IFile file = EditorUtil.getFile(editor);
            if (file != null) {
                return KotlinPsiManager.INSTANCE.getParsedFile(file);
            } 
            
            KotlinLogger.logError("Failed to retrieve IFile from editor " + editor, null);
            return null;
        }
    }
    
    @Override
    protected Map<String, IAdaptable> getHyperlinkDetectorTargets(ISourceViewer sourceViewer) {
        Map<String, IAdaptable> targets = super.getHyperlinkDetectorTargets(sourceViewer);
        targets.remove("org.eclipse.jdt.ui.javaCode");
        targets.put("org.jetbrains.kotlin.ui.editors.kotlinCode", getEditor());
        
        return targets;
    }
    
    public static KotlinPresentationReconciler getKotlinPresentaionReconciler(@NotNull KotlinTokenScanner scanner) {
        DefaultDamagerRepairer kotlinDamagerRepairer = new DefaultDamagerRepairer(scanner);
        
        KotlinPresentationReconciler reconciler = new KotlinPresentationReconciler();
        reconciler.setDamager(kotlinDamagerRepairer, IDocument.DEFAULT_CONTENT_TYPE);
        reconciler.setRepairer(kotlinDamagerRepairer, IDocument.DEFAULT_CONTENT_TYPE);
        
        return reconciler;
    }
    
    public static class KotlinPresentationReconciler extends PresentationReconciler {
        private volatile IDocument lastDocument = null;
        
        public TextPresentation createRepairDescription(IRegion damage, IDocument document) {
            if (document != lastDocument) {
                setDocumentToDamagers(document);
                setDocumentToRepairers(document);
                lastDocument = document;
            }
            
            return createPresentation(damage, document);
        }
    }
}