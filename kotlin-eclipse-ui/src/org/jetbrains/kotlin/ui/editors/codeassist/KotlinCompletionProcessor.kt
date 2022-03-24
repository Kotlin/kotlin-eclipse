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

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.internal.ui.JavaPlugin
import org.eclipse.jdt.internal.ui.JavaPluginImages
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider
import org.eclipse.jface.text.IRegion
import org.eclipse.jface.text.ITextViewer
import org.eclipse.jface.text.Region
import org.eclipse.jface.text.contentassist.*
import org.eclipse.jface.text.templates.TemplateContext
import org.eclipse.jface.text.templates.TemplateProposal
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.eclipse.ui.utils.KotlinImageProvider
import org.jetbrains.kotlin.idea.util.CallTypeAndReceiver
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.ui.editors.KotlinEditor
import org.jetbrains.kotlin.ui.editors.completion.KotlinCompletionUtils
import org.jetbrains.kotlin.ui.editors.templates.KotlinApplicableTemplateContext
import org.jetbrains.kotlin.ui.editors.templates.KotlinDocumentTemplateContext
import org.jetbrains.kotlin.ui.editors.templates.KotlinTemplateManager

sealed class KotlinBasicCompletionProposal() {

    abstract val descriptor: DeclarationDescriptor

    class Proposal(val proposal: KotlinCompletionProposal, override val descriptor: DeclarationDescriptor) :
        KotlinBasicCompletionProposal()

    class Descriptor(override val descriptor: DeclarationDescriptor) : KotlinBasicCompletionProposal()
}

abstract class KotlinCompletionProcessor(
    val editor: KotlinEditor,
    private val assistant: ContentAssistant?,
    private val needSorting: Boolean
) : IContentAssistProcessor, ICompletionListener {

    companion object {
        private val VALID_PROPOSALS_CHARS = charArrayOf()
        private val VALID_INFO_CHARS = charArrayOf('(', ',')
        fun createKotlinCompletionProcessors(
            editor: KotlinEditor,
            assistant: ContentAssistant? = null,
            needSorting: Boolean = false
        ) = listOf<IContentAssistProcessor>(
            object : KotlinCompletionProcessor(editor, assistant, needSorting) {
                override fun computeProposals(
                    identifierPart: String,
                    psiElement: PsiElement?,
                    simpleNameExpression: KtSimpleNameExpression?,
                    viewer: ITextViewer,
                    offset: Int
                ): List<ICompletionProposal>? =
                    simpleNameExpression?.let {
                        collectCompletionProposals(
                            generateBasicCompletionProposals(identifierPart, simpleNameExpression),
                            identifierPart
                        )
                    }
            },
            object : KotlinCompletionProcessor(editor, assistant, needSorting) {
                override fun computeProposals(
                    identifierPart: String,
                    psiElement: PsiElement?,
                    simpleNameExpression: KtSimpleNameExpression?,
                    viewer: ITextViewer,
                    offset: Int
                ): List<ICompletionProposal>? =
                    simpleNameExpression?.takeIf { identifierPart.isNotBlank() }?.let {
                        generateNonImportedCompletionProposals(
                            identifierPart,
                            simpleNameExpression,
                            editor.javaProject!!
                        )
                    }
            },
            object : KotlinCompletionProcessor(editor, assistant, needSorting) {
                override fun computeProposals(
                    identifierPart: String,
                    psiElement: PsiElement?,
                    simpleNameExpression: KtSimpleNameExpression?,
                    viewer: ITextViewer,
                    offset: Int
                ): List<ICompletionProposal>? =
                    psiElement?.let {
                        generateKeywordProposals(identifierPart, psiElement)
                    }
            },
            object : KotlinCompletionProcessor(editor, assistant, needSorting) {
                override fun computeProposals(
                    identifierPart: String,
                    psiElement: PsiElement?,
                    simpleNameExpression: KtSimpleNameExpression?,
                    viewer: ITextViewer,
                    offset: Int
                ): List<ICompletionProposal>? =
                    psiElement?.let {
                        generateTemplateProposals(psiElement.containingFile, viewer, offset, identifierPart)
                    }
            }
        )
    }

    private val kotlinParameterValidator by lazy {
        KotlinParameterListValidator(editor)
    }

    override fun computeCompletionProposals(viewer: ITextViewer, offset: Int): Array<ICompletionProposal> {
        if (assistant != null) {
            configureContentAssistant(assistant)
        }

        val generatedProposals = generateCompletionProposals(viewer, offset).let {
            if (needSorting) sortProposals(it) else it
        }

        return generatedProposals.toTypedArray()
    }

    private fun sortProposals(proposals: List<ICompletionProposal>): List<ICompletionProposal> {
        return proposals.sortedWith(KotlinCompletionSorter::compare)
    }

    private fun configureContentAssistant(contentAssistant: ContentAssistant) {
        contentAssistant.setEmptyMessage("No Default Proposals")
        contentAssistant.setSorter(KotlinCompletionSorter)
    }

    private fun generateCompletionProposals(viewer: ITextViewer, offset: Int): List<ICompletionProposal> {
        val (identifierPart, identifierStart) = getIdentifierInfo(viewer.document, offset)
        val psiElement = KotlinCompletionUtils.getPsiElement(editor, identifierStart)
        val simpleNameExpression = PsiTreeUtil.getParentOfType(psiElement, KtSimpleNameExpression::class.java)

        return computeProposals(identifierPart, psiElement, simpleNameExpression, viewer, offset) ?: emptyList()
    }

    abstract fun computeProposals(
        identifierPart: String,
        psiElement: PsiElement?,
        simpleNameExpression: KtSimpleNameExpression?,
        viewer: ITextViewer,
        offset: Int
    ): List<ICompletionProposal>?

    protected fun generateNonImportedCompletionProposals(
        identifierPart: String,
        expression: KtSimpleNameExpression,
        javaProject: IJavaProject
    ): List<KotlinCompletionProposal> {
        val file = editor.eclipseFile ?: return emptyList()
        val ktFile = editor.parsedFile ?: return emptyList()

        val tempTypeProposals = lookupNonImportedTypes(expression, identifierPart, ktFile, javaProject).map {
            val imageDescriptor = JavaElementImageProvider.getTypeImageDescriptor(false, false, it.modifiers, false)
            val image = JavaPlugin.getImageDescriptorRegistry().get(imageDescriptor)

            KotlinImportTypeCompletionProposal(it, image, file, identifierPart)
        }

        return tempTypeProposals
    }

    protected fun generateBasicCompletionProposals(
        identifierPart: String,
        expression: KtSimpleNameExpression
    ): Collection<KotlinBasicCompletionProposal> {
        val file = editor.eclipseFile ?: throw IllegalStateException("Failed to retrieve IFile from editor $editor")
        val ktFile = editor.parsedFile ?: throw IllegalStateException("Failed to retrieve KTFile from editor $editor")

        val nameFilter: (Name) -> Boolean = { name -> KotlinCompletionUtils.applicableNameFor(identifierPart, name) }

        return KotlinCompletionUtils.getReferenceVariants(
            expression,
            nameFilter,
            ktFile,
            file,
            identifierPart,
            editor.javaProject!!
        )
    }

    protected fun collectCompletionProposals(
        descriptors: Collection<KotlinBasicCompletionProposal>,
        part: String
    ): List<KotlinCompletionProposal> {
        return descriptors.map { basicDescriptor ->
            when (basicDescriptor) {
                is KotlinBasicCompletionProposal.Descriptor -> {
                    val descriptor = basicDescriptor.descriptor
                    val completion = descriptor.name.identifier
                    val image = KotlinImageProvider.getImage(descriptor)

                    val tempRenderer = DescriptorRenderer.SHORT_NAMES_IN_TYPES.withOptions {
                        modifiers = emptySet()
                        includeAdditionalModifiers = false
                    }

                    val presentableString = tempRenderer.render(descriptor)
                    val containmentPresentableString = null

                    val proposal = KotlinCompletionProposal(
                        completion,
                        image,
                        presentableString,
                        containmentPresentableString,
                        null,
                        completion,
                        part
                    )

                    withKotlinInsertHandler(descriptor, proposal, part)
                }
                is KotlinBasicCompletionProposal.Proposal -> basicDescriptor.proposal
            }
        }
    }

    protected fun generateTemplateProposals(
        psiFile: PsiFile, viewer: ITextViewer, offset: Int, identifierPart: String
    ): List<ICompletionProposal> {

        val contextTypeIds =
            KotlinApplicableTemplateContext.getApplicableContextTypeIds(viewer, psiFile, offset - identifierPart.length)
        val region = Region(offset - identifierPart.length, identifierPart.length)

        val templateIcon = JavaPluginImages.get(JavaPluginImages.IMG_OBJS_TEMPLATE)
        val templates = KotlinApplicableTemplateContext.getTemplatesByContextTypeIds(contextTypeIds)

        return templates
            .filter { it.name.startsWith(identifierPart) }
            .map {
                val templateContext = createTemplateContext(region, it.contextTypeId)
                TemplateProposal(it, templateContext, region, templateIcon)
            }

    }

    private fun createTemplateContext(region: IRegion, contextTypeID: String): TemplateContext {
        return KotlinDocumentTemplateContext(
            KotlinTemplateManager.INSTANCE.contextTypeRegistry.getContextType(contextTypeID),
            editor, region.offset, region.length
        )
    }

    protected fun generateKeywordProposals(
        identifierPart: String,
        expression: PsiElement
    ): List<KotlinCompletionProposal> {
        val callTypeAndReceiver =
            if (expression is KtSimpleNameExpression) CallTypeAndReceiver.detect(expression) else null

        return arrayListOf<String>().apply {
            KeywordCompletion.complete(expression, identifierPart, true) { keywordProposal ->
                if (!KotlinCompletionUtils.applicableNameFor(identifierPart, keywordProposal)) return@complete

                when (keywordProposal) {
                    "break", "continue" -> {
                        if (expression is KtSimpleNameExpression) {
                            addAll(breakOrContinueExpressionItems(expression, keywordProposal))
                        }
                    }

                    "class" -> {
                        if (callTypeAndReceiver !is CallTypeAndReceiver.CALLABLE_REFERENCE) {
                            add(keywordProposal)
                        }
                    }

                    "this", "return" -> {
                        if (expression is KtExpression) {
                            add(keywordProposal)
                        }
                    }

                    else -> add(keywordProposal)
                }
            }
        }.map { KotlinKeywordCompletionProposal(it, identifierPart) }
    }

    override fun computeContextInformation(viewer: ITextViewer?, offset: Int): Array<IContextInformation> {
        return KotlinFunctionParameterInfoAssist.computeContextInformation(editor, offset)
    }

    override fun getCompletionProposalAutoActivationCharacters(): CharArray = VALID_PROPOSALS_CHARS

    override fun getContextInformationAutoActivationCharacters(): CharArray = VALID_INFO_CHARS

    override fun getErrorMessage(): String? = ""

    override fun getContextInformationValidator(): IContextInformationValidator = kotlinParameterValidator

    override fun assistSessionStarted(event: ContentAssistEvent?) {
    }

    override fun assistSessionEnded(event: ContentAssistEvent?) {
    }

    override fun selectionChanged(proposal: ICompletionProposal?, smartToggle: Boolean) {}
}

private object KotlinCompletionSorter : ICompletionProposalSorter {
    override fun compare(p1: ICompletionProposal, p2: ICompletionProposal): Int {
        val relevance2 = p2.relevance()
        val relevance1 = p1.relevance()

        return when {
            relevance2 > relevance1 -> 1
            relevance2 < relevance1 -> -1
            else -> p1.sortString().compareTo(p2.sortString(), ignoreCase = true)
        }
    }

    private fun ICompletionProposal.sortString(): String {
        return if (this is KotlinCompletionProposal) this.replacementString else this.displayString
    }

    private fun ICompletionProposal.relevance(): Int {
        return if (this is KotlinCompletionProposal) this.getRelevance() else 0
    }
}
