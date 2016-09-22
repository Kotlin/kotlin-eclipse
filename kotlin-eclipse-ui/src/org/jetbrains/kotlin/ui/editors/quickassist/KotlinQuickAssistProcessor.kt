package org.jetbrains.kotlin.ui.editors.quickassist

import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal
import org.jetbrains.kotlin.ui.editors.KotlinEditor

object KotlinQuickAssistProcessor {
    fun getAssists(editor: KotlinEditor) : List<KotlinQuickAssistProposal> {
        return getSingleKotlinQuickAssistProposals(editor)
                .filter { it.isApplicable() }
    }
    
    private fun getSingleKotlinQuickAssistProposals(editor: KotlinEditor) : List<KotlinQuickAssistProposal> {
        return listOf(
            KotlinReplaceGetAssistProposal(editor), 
            KotlinSpecifyTypeAssistProposal(editor),
            KotlinRemoveExplicitTypeAssistProposal(editor),
            KotlinImplementMethodsProposal(editor),
            KotlinConvertToExpressionBodyAssistProposal(editor),
            KotlinConvertToBlockBodyAssistProposal(editor),
            KotlinChangeReturnTypeProposal(editor))
    }
}