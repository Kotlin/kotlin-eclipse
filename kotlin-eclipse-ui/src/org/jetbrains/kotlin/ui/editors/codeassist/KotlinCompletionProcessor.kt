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
package org.jetbrains.kotlin.ui.editors.codeassist

import java.util.ArrayList
import java.util.Collections
import org.eclipse.core.resources.IFile
import org.eclipse.core.runtime.Assert
import org.eclipse.jdt.internal.ui.JavaPluginImages
import org.eclipse.jface.text.IRegion
import org.eclipse.jface.text.ITextViewer
import org.eclipse.jface.text.Region
import org.eclipse.jface.text.contentassist.CompletionProposal
import org.eclipse.jface.text.contentassist.ContentAssistEvent
import org.eclipse.jface.text.contentassist.ICompletionListener
import org.eclipse.jface.text.contentassist.ICompletionProposal
import org.eclipse.jface.text.contentassist.IContentAssistProcessor
import org.eclipse.jface.text.contentassist.IContextInformation
import org.eclipse.jface.text.contentassist.IContextInformationValidator
import org.eclipse.jface.text.templates.Template
import org.eclipse.jface.text.templates.TemplateContext
import org.eclipse.jface.text.templates.TemplateProposal
import org.eclipse.swt.graphics.Image
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.jetbrains.kotlin.eclipse.ui.utils.KotlinImageProvider
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.jetbrains.kotlin.ui.editors.completion.KotlinCompletionUtils
import org.jetbrains.kotlin.ui.editors.templates.KotlinApplicableTemplateContext
import org.jetbrains.kotlin.ui.editors.templates.KotlinDocumentTemplateContext
import org.jetbrains.kotlin.ui.editors.templates.KotlinTemplateManager
import com.intellij.psi.tree.IElementType
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider
import org.eclipse.jface.resource.ImageDescriptor
import org.eclipse.jface.viewers.StyledString
import org.eclipse.jdt.ui.JavaElementLabels
import org.eclipse.jdt.internal.corext.util.Strings
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.eclipse.jface.text.IDocument
import org.jetbrains.kotlin.ui.editors.quickfix.placeImports
import org.eclipse.swt.graphics.Point
import org.jetbrains.kotlin.descriptors.FunctionDescriptor

class KotlinCompletionProcessor(private val editor: KotlinFileEditor) : IContentAssistProcessor, ICompletionListener {
    companion object {
        private val VALID_PROPOSALS_CHARS = charArrayOf('.')
        private val VALID_INFO_CHARS = charArrayOf('(', ',')
        
        private val descriptorsToImages = hashMapOf<ImageDescriptor, Image>()
    }
    
    private val cachedProposals = arrayListOf<ProposalWithCompletion>()
    
    private val kotlinParameterValidator by lazy {
        KotlinParameterListValidator(editor)
    }
    
    private var isNewSession = true
    
    override fun computeCompletionProposals(viewer: ITextViewer, offset: Int): Array<ICompletionProposal> {
        val fileText = viewer.getDocument().get()
        val identOffset = getIdentifierStartOffset(fileText, offset)
        
        val identifierPart = fileText!!.substring(identOffset, offset)
        if (isNewSession) {
            cachedProposals.clear()
            cachedProposals.addAll(generateCompletionProposals(viewer, identifierPart, offset, identOffset))
            isNewSession = false
        }
        
        return cachedProposals
                .filter { KotlinCompletionUtils.applicableNameFor(identifierPart, it.completion) }
                .map { it.proposal }
                .toTypedArray()
    }
    
    private fun generateCompletionProposals(
            viewer: ITextViewer, 
            identifierPart: String, 
            offset: Int, 
            identOffset: Int): List<ProposalWithCompletion> {
        val expression = KotlinCompletionUtils.getSimpleNameExpression(editor, identOffset)
        
        return arrayListOf<ProposalWithCompletion>().apply {
            if (expression != null) {
                addAll(collectCompletionProposals(generateBasicCompletionProposals(identifierPart, expression)))
                addAll(generateNonImportedCompletionProposals(identifierPart, expression, editor.javaProject!!))
            }
            addAll(generateKeywordProposals(identOffset, offset, identifierPart))
            addAll(generateTemplateProposals(viewer, offset, identifierPart))
        }
    }
    
    private fun generateNonImportedCompletionProposals(
            identifierPart: String, 
            expression: KtSimpleNameExpression,
            javaProject: IJavaProject): List<ProposalWithCompletion> {
        
        val file = editor.getFile() ?: return emptyList()
        val ktFile = editor.parsedFile ?: return emptyList()
        val nonImportedTypesVariants = lookupNonImportedTypes(expression, identifierPart, ktFile, javaProject)
        return nonImportedTypesVariants.map { 
            val completion = it.simpleTypeName
            val imageDescriptor = JavaElementImageProvider.getTypeImageDescriptor(false, false, it.type.flags, false)
            val image = descriptorsToImages.getOrPut(imageDescriptor) { imageDescriptor.createImage() }
            
            val proposal = KotlinImportCompletionProposal(it, image, file)
            
            ProposalWithCompletion(proposal, completion)
        }
    }
    
    private fun generateBasicCompletionProposals(identifierPart: String, expression: KtSimpleNameExpression): Collection<DeclarationDescriptor> {
        val file = EditorUtil.getFile(editor)
        if (file == null) {
            throw IllegalStateException("Failed to retrieve IFile from editor $editor")
        }
        
        val nameFilter: (Name) -> Boolean = { name -> KotlinCompletionUtils.applicableNameFor(identifierPart, name) }
        
        return KotlinCompletionUtils.getReferenceVariants(expression, nameFilter, file)
    }
    
    private fun collectCompletionProposals(descriptors: Collection<DeclarationDescriptor>): List<ProposalWithCompletion> {
                        
        return descriptors.map { descriptor ->
            val completion = descriptor.name.identifier
            val image = KotlinImageProvider.getImage(descriptor)!!
            val presentableString = DescriptorRenderer.ONLY_NAMES_WITH_SHORT_TYPES.render(descriptor)
            val containmentPresentableString = if (descriptor is ClassDescriptor) {
                    val fqName = DescriptorUtils.getFqName(descriptor)
                    if (fqName.isRoot) "<root>" else fqName.parent().asString()
                } else 
                    null
            
            val proposal = KotlinCompletionProposal(
                                completion,
                                image,
                                presentableString,
                                containmentPresentableString,
                                null,
                                completion)
            
            ProposalWithCompletion(withKotlinInsertHandler(descriptor, proposal), completion)
        }
    }
    
    private fun generateTemplateProposals(viewer: ITextViewer, offset: Int, identifierPart: String): List<ProposalWithCompletion> {
        val file = EditorUtil.getFile(editor)
        if (file == null) {
            KotlinLogger.logError("Failed to retrieve IFile from editor $editor", null)
            return emptyList()
        }
        
        val contextTypeIds = KotlinApplicableTemplateContext.getApplicableContextTypeIds(viewer, file, offset - identifierPart.length)
        val region = Region(offset - identifierPart.length, identifierPart.length)
        
        val templateIcon = JavaPluginImages.get(JavaPluginImages.IMG_OBJS_TEMPLATE)
        val templates = KotlinApplicableTemplateContext.getTemplatesByContextTypeIds(contextTypeIds)
        
        return templates
                .filter { it.name.startsWith(identifierPart) }
                .map {
                    val templateContext = createTemplateContext(region, it.contextTypeId)
                    ProposalWithCompletion(
                            TemplateProposal(it, templateContext, region, templateIcon),
                            it.name)
                }
        
    }
    
    private fun createTemplateContext(region: IRegion, contextTypeID: String): TemplateContext {
        return KotlinDocumentTemplateContext(
                KotlinTemplateManager.INSTANCE.getContextTypeRegistry().getContextType(contextTypeID),
                editor, region.getOffset(), region.getLength())
    }
    
    private fun generateKeywordProposals(
            identOffset: Int, 
            offset: Int, 
            identifierPart: String): List<ProposalWithCompletion> {
        if (identifierPart.isBlank()) return emptyList()
        
        return KtTokens.KEYWORDS.types
                .filter { it.toString().startsWith(identifierPart) }
                .map { 
                    val keyword = it.toString()
                    ProposalWithCompletion(
                            CompletionProposal(keyword, identOffset, offset - identOffset, keyword.length),
                            keyword)
                }
    }
    
    private fun getIdentifierStartOffset(text: String, offset: Int): Int {
        var identStartOffset = offset
        while ((identStartOffset != 0) && Character.isUnicodeIdentifierPart(text[identStartOffset - 1])) {
            identStartOffset--
        }
        return identStartOffset
    }
    
    override fun computeContextInformation(viewer: ITextViewer?, offset: Int): Array<IContextInformation> {
        return KotlinFunctionParameterInfoAssist.computeContextInformation(editor, offset)
    }
    
    override fun getCompletionProposalAutoActivationCharacters(): CharArray = VALID_PROPOSALS_CHARS
    
    override fun getContextInformationAutoActivationCharacters(): CharArray = VALID_INFO_CHARS
    
    override fun getErrorMessage(): String? = ""
    
    override fun getContextInformationValidator(): IContextInformationValidator = kotlinParameterValidator
    
    override fun assistSessionStarted(event: ContentAssistEvent?) {
        isNewSession = true
    }
    
    override fun assistSessionEnded(event: ContentAssistEvent?) {
        cachedProposals.clear()
    }
    
    override fun selectionChanged(proposal: ICompletionProposal?, smartToggle: Boolean) { }
}

private class ProposalWithCompletion(val proposal: ICompletionProposal, val completion: String)