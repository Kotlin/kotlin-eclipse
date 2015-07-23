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

import kotlin.jvm.functions.Function1;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.ui.PreferenceConstants;
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
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.model.KotlinEnvironment;
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil;
import org.jetbrains.kotlin.idea.codeInsight.ReferenceVariantsHelper;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.JetSimpleNameExpression;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.ui.editors.KotlinEditor;
import org.jetbrains.kotlin.ui.editors.completion.KotlinCompletionUtils;
import org.jetbrains.kotlin.ui.editors.completion.KotlinDescriptorUtils;
import org.jetbrains.kotlin.ui.editors.templates.KotlinApplicableTemplateContext;
import org.jetbrains.kotlin.ui.editors.templates.KotlinDocumentTemplateContext;
import org.jetbrains.kotlin.ui.editors.templates.KotlinTemplateManager;

import com.google.common.collect.Lists;
import com.intellij.psi.tree.IElementType;

public class KotlinCompletionProcessor implements IContentAssistProcessor, ICompletionListener {
     
    /**
     * Characters for auto activation proposal computation.
     */
    private static final char[] VALID_PROPOSALS_CHARS = new char[] { '.' };
    private static final char[] VALID_INFO_CHARS = new char[] { '(', ',' };
    
    private final KotlinEditor editor;
    private final List<DeclarationDescriptor> cachedDescriptors = Lists.newArrayList();
    private boolean isNewSession = true;
    
    public KotlinCompletionProcessor(KotlinEditor editor) {
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
        
        int identOffset = getIdentifierStartOffset(fileText, offset);
        Assert.isTrue(identOffset <= offset);
        
        String identifierPart = fileText.substring(identOffset, offset);
        
        if (isNewSession) {
            cachedDescriptors.clear();
            cachedDescriptors.addAll(generateBasicCompletionProposals(viewer, identOffset));
            
            isNewSession = false;
        }
        
        List<ICompletionProposal> proposals = Lists.newArrayList();
        
        proposals.addAll(
                collectCompletionProposals(
                        KotlinCompletionUtils.INSTANCE.filterCompletionProposals(cachedDescriptors, identifierPart),
                        identOffset,
                        offset - identOffset));
        proposals.addAll(generateKeywordProposals(viewer, identOffset, offset, identifierPart));
        proposals.addAll(generateTemplateProposals(viewer, offset, identifierPart));
        
        return proposals.toArray(new ICompletionProposal[proposals.size()]);
    }
    
    @NotNull
    private Collection<DeclarationDescriptor> generateBasicCompletionProposals(@NotNull ITextViewer viewer, int identOffset) {
        JetSimpleNameExpression simpleNameExpression = KotlinCompletionUtils.INSTANCE.getSimpleNameExpression(editor, identOffset);
        if (simpleNameExpression == null) {
            return Collections.emptyList();
        }
        
        IFile file = EditorUtil.getFile(editor);
        assert file != null : "Failed to retrieve IFile from editor " + editor;
        
        return getReferenceVariants(simpleNameExpression, file);
    }
    
    @NotNull
    private Collection<DeclarationDescriptor> getReferenceVariants(
            @NotNull final JetSimpleNameExpression simpleNameExpression,
            @NotNull IFile file) {
        IJavaProject javaProject = JavaCore.create(file.getProject());
        final AnalysisResult analysisResult = KotlinAnalyzer.analyzeFile(javaProject,
                simpleNameExpression.getContainingJetFile());
        final String expressionName = KotlinCompletionUtils.INSTANCE.replaceMarkerInIdentifier(simpleNameExpression.getReferencedName());
        
        JetScope resolutionScope = CodeassistPackage.getResolutionScope(
                simpleNameExpression.getReferencedNameElement(), analysisResult.getBindingContext());
        
        final DeclarationDescriptor inDescriptor = resolutionScope.getContainingDeclaration();
        
        final boolean showNonVisibleMembers = !JavaPlugin.getDefault().getPreferenceStore().getBoolean(
                PreferenceConstants.CODEASSIST_SHOW_VISIBLE_PROPOSALS);
        
        Function1<DeclarationDescriptor, Boolean> visibilityFilter = new Function1<DeclarationDescriptor, Boolean>() {
            @Override
            public Boolean invoke(DeclarationDescriptor descriptor) {
                if (descriptor instanceof TypeParameterDescriptor) {
                    if (!CodeassistPackage.isVisible((TypeParameterDescriptor) descriptor, inDescriptor)) {
                        return false;
                    }
                }
                if (descriptor instanceof DeclarationDescriptorWithVisibility) {
                    boolean visible = CodeassistPackage.isVisible((DeclarationDescriptorWithVisibility) descriptor,
                            inDescriptor, analysisResult.getBindingContext(), simpleNameExpression);
                    return visible || showNonVisibleMembers;
                }
                return true;
            }
        };
        
        Function1<Name, Boolean> nameFilter = new Function1<Name, Boolean>() {
            @Override
            public Boolean invoke(Name name) {
                String nameString = name.asString();
                return nameString.startsWith(expressionName);
            }
        };
        
        return new ReferenceVariantsHelper(
                analysisResult.getBindingContext(), 
                analysisResult.getModuleDescriptor(),
                KotlinEnvironment.getEnvironment(javaProject).getProject(),
                visibilityFilter).getReferenceVariants(
                simpleNameExpression, DescriptorKindFilter.ALL, nameFilter, false, false);
    }
    
    private List<ICompletionProposal> collectCompletionProposals(
            @NotNull Collection<DeclarationDescriptor> descriptors,
            int replacementOffset,
            int replacementLength) {
        List<ICompletionProposal> proposals = Lists.newArrayList();
        for (DeclarationDescriptor descriptor : descriptors) {
            String completion = descriptor.getName().getIdentifier();
            Image image = KotlinDescriptorUtils.INSTANCE.getImage(descriptor);
            String presentableString = DescriptorRenderer.ONLY_NAMES_WITH_SHORT_TYPES.render(descriptor);
            assert image != null : "Image for completion must not be null";
            
            KotlinCompletionProposal proposal = new KotlinCompletionProposal(
                    completion,
                    replacementOffset, 
                    replacementLength, 
                    completion.length(), 
                    image, 
                    presentableString, 
                    null, 
                    completion);
            
            ICompletionProposal handler = CodeassistPackage.withKotlinInsertHandler(descriptor, proposal);
            proposals.add(handler);
        }
        
        return proposals;
    }
    
    private Collection<ICompletionProposal> generateTemplateProposals(ITextViewer viewer, int offset, String identifierPart) {
        IFile file = EditorUtil.getFile(editor);
        if (file == null) {
            KotlinLogger.logError("Failed to retrieve IFile from editor " + editor, null);
            return Collections.emptyList();
        }

        List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();
        List<String> contextTypeIds = KotlinApplicableTemplateContext.getApplicableContextTypeIds(viewer, file, offset - identifierPart.length());

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
            for (IElementType keywordToken : JetTokens.KEYWORDS.getTypes()) {
                String keyword = keywordToken.toString();
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
        cachedDescriptors.clear();
    }

    @Override
    public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {
    }

}