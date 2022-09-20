package org.jetbrains.kotlin.ui.editors.codeassist

import org.eclipse.jface.text.ITextViewer
import org.eclipse.jface.text.contentassist.ICompletionProposal
import org.eclipse.jface.text.contentassist.IContentAssistProcessor
import org.eclipse.jface.text.contentassist.IContextInformation
import org.jetbrains.kotlin.ui.editors.KotlinEditor

class KotlinContextInfoContentAssistProcessor(private val editor: KotlinEditor) : IContentAssistProcessor {

    private val kotlinParameterValidator by lazy {
        KotlinParameterListValidator(editor)
    }

    override fun computeCompletionProposals(p0: ITextViewer?, p1: Int): Array<ICompletionProposal> = emptyArray()

    override fun computeContextInformation(p0: ITextViewer?, offset: Int): Array<IContextInformation> {
        return KotlinFunctionParameterInfoAssist.computeContextInformation(editor, offset)
    }

    override fun getCompletionProposalAutoActivationCharacters() = charArrayOf()

    override fun getContextInformationAutoActivationCharacters() = VALID_INFO_CHARS

    override fun getErrorMessage() = ""

    override fun getContextInformationValidator() = kotlinParameterValidator

    companion object {
        private val VALID_INFO_CHARS = charArrayOf('(', ',')
    }
}
