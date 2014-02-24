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
package org.jetbrains.kotlin.ui.editors.codeassist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateProposal;
import org.eclipse.swt.graphics.Image;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.renderer.DescriptorRenderer;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer;
import org.jetbrains.kotlin.ui.editors.KeywordManager;
import org.jetbrains.kotlin.ui.editors.completion.KotlinCompletionProvider;
import org.jetbrains.kotlin.ui.editors.completion.KotlinCompletionUtils;
import org.jetbrains.kotlin.ui.editors.completion.KotlinDescriptorUtils;
import org.jetbrains.kotlin.ui.editors.templates.KotlinApplicableTemplateContext;
import org.jetbrains.kotlin.ui.editors.templates.KotlinDocumentTemplateContext;
import org.jetbrains.kotlin.ui.editors.templates.KotlinTemplateManager;
import org.jetbrains.kotlin.utils.EditorUtil;

import com.google.common.collect.Lists;

public class CompletionProcessor implements IContentAssistProcessor, ICompletionListener {
     
    /**
     * Characters for auto activation proposal computation.
     */
    private static final char[] VALID_PROPOSALS_CHARS = new char[] { '.' };
    private static final char[] VALID_INFO_CHARS = new char[] { '(', ',' };
    
    private final JavaEditor editor;
    private final List<ICompletionProposal> cachedCompletionProposals = Lists.newArrayList();
    private boolean isNewSession = true;
    
    public CompletionProcessor(JavaEditor editor) {
        this.editor = editor;
    }
    
    /**
     * A very simple context which invalidates information after typing several
     * chars.
     */
    private static class KotlinContextValidator implements IContextInformationValidator {
        private int initialOffset;
        
        @Override
        public void install(IContextInformation info, ITextViewer viewer, int offset) {
            this.initialOffset = offset;
        }

        @Override
        public boolean isContextInformationValid(int offset) {
            return Math.abs(initialOffset - offset) < 1;
        }
    }
    
    @Override
    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
        String fileText = viewer.getDocument().get();
        
        KotlinPsiManager.INSTANCE.updatePsiFile(EditorUtil.getFile(editor), fileText);
        
        int identOffset = getIdentifierStartOffset(fileText, offset);
        Assert.isTrue(identOffset <= offset);
        
        String identifierPart = fileText.substring(identOffset, offset);
        
        if (isNewSession) {
            cachedCompletionProposals.clear();
            cachedCompletionProposals.addAll(generateBasicCompletionProposals(viewer, identOffset, offset, identifierPart));
            
            isNewSession = false;
        }
        
        List<ICompletionProposal> proposals = Lists.newArrayList();
        
        proposals.addAll(KotlinCompletionUtils.INSTANCE.filterCompletionProposals(cachedCompletionProposals, identifierPart, identOffset));
        proposals.addAll(generateKeywordProposals(viewer, identOffset, offset, identifierPart));
        proposals.addAll(generateTemplateProposals(viewer, offset, identifierPart));
        
        return proposals.toArray(new ICompletionProposal[proposals.size()]);
    }
    
    @NotNull
    private Collection<ICompletionProposal> generateBasicCompletionProposals(@NotNull ITextViewer viewer, int identOffset, 
            int offset, @NotNull String identifierPart) {
        JetSimpleNameExpression simpleNameExpression = KotlinCompletionUtils.INSTANCE.getSimpleNameExpression(editor, identOffset);
        if (simpleNameExpression == null) {
            return Collections.emptyList();
        }
        
        IFile file = EditorUtil.getFile(editor);
        IJavaProject javaProject = JavaCore.create(file.getProject());
        
        BindingContext context = KotlinAnalyzer.analyzeOnlyOneFileCompletely(javaProject, KotlinPsiManager.INSTANCE.getParsedFile(file));
        
        Collection<DeclarationDescriptor> declarationDescriptors = KotlinCompletionProvider.getReferenceVariants(simpleNameExpression, context);
        
        List<ICompletionProposal> proposals = Lists.newArrayList();
        for (DeclarationDescriptor descriptor : declarationDescriptors) {
            String completion = descriptor.getName().getIdentifier();
            Image image = KotlinDescriptorUtils.INSTANCE.getImage(descriptor);
            String presentableString = DescriptorRenderer.STARTS_FROM_NAME.render(descriptor);
            
            proposals.add(new CompletionProposal(completion, identOffset, offset - identOffset, completion.length(), image, presentableString, null, completion));
        }
        
        return proposals;
    }
    
    private Collection<ICompletionProposal> generateTemplateProposals(ITextViewer viewer, int offset, String identifierPart) {
        List<String> contextTypeIds = KotlinApplicableTemplateContext.getApplicableContextTypeIds(viewer, 
                EditorUtil.getFile(editor), offset - identifierPart.length());
        
        List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();
        IRegion region = new Region(offset - identifierPart.length(), identifierPart.length());
        Image templateIcon = JavaPluginImages.get(JavaPluginImages.IMG_OBJS_TEMPLATE);
        
        List<Template> templates = KotlinApplicableTemplateContext.getTemplatesByContextTypeIds(contextTypeIds);
        for (Template template : templates) {
            if (template.getName().startsWith(identifierPart)) {
                TemplateContext templateContext = createTemplateContext(region, template.getContextTypeId());
                proposals.add(new TemplateProposal(template, templateContext, region, templateIcon));
            }
        }
        
        return proposals;
    }
    
    private TemplateContext createTemplateContext(IRegion region, String contextTypeID) {
        return new KotlinDocumentTemplateContext(
                KotlinTemplateManager.INSTANCE.getContextTypeRegistry().getContextType(contextTypeID), 
                editor, region.getOffset(), region.getLength());
    }

    /**
     * Generate list of matching keywords
     * 
     * @param viewer the viewer whose document is used to compute the proposals
     * @param identOffset an offset within the document for which completions should be computed 
     * @param offset current position id the document
     * @param identifierPart part of current keyword
     * @return a collection of matching keywords  
     */
    private Collection<? extends ICompletionProposal> generateKeywordProposals(ITextViewer viewer, int identOffset,
            int offset, String identifierPart) {
        List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();
        if (!identifierPart.isEmpty()) {
            for (String keyword : KeywordManager.getAllKeywords()) {
                if (keyword.startsWith(identifierPart)) {
                    proposals.add(new CompletionProposal(keyword, identOffset, offset - identOffset, keyword.length()));
                }
            }
        }
        return proposals;
    }

    /**
     * Method searches the beginning of the identifier 
     * 
     * @param text the text where search should be done.
     * @param offset 
     * @return offset of start symbol of identifier
     */
    private int getIdentifierStartOffset(String text, int offset) {
        int identStartOffset = offset;
        
        while ((identStartOffset != 0) && Character.isUnicodeIdentifierPart(text.charAt(identStartOffset - 1))) {
            identStartOffset--;
        }
        return identStartOffset;
    }
    
    
    @Override
    public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
        return null;
    }

    @Override
    public char[] getCompletionProposalAutoActivationCharacters() {
        return VALID_PROPOSALS_CHARS;
    }

    @Override
    public char[] getContextInformationAutoActivationCharacters() {
        return VALID_INFO_CHARS;
    }

    @Override
    public String getErrorMessage() {
        return "";
    }

    @Override
    public IContextInformationValidator getContextInformationValidator() {
        return new KotlinContextValidator();
    }

    @Override
    public void assistSessionStarted(ContentAssistEvent event) {
        isNewSession = true;
    }

    @Override
    public void assistSessionEnded(ContentAssistEvent event) {
    }

    @Override
    public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {
    }

}