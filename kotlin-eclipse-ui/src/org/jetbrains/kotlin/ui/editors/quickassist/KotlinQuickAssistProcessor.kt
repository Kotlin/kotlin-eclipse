package org.jetbrains.kotlin.ui.editors.quickassist

import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal
import org.jetbrains.kotlin.ui.editors.KotlinEditor

object KotlinQuickAssistProcessor {
    fun getAssists(editor: KotlinEditor) : List<KotlinQuickAssistProposal> {
        return getSingleKotlinQuickAssistProposals(editor)
                .filter { it.isApplicable() }
    }
    
    private fun getSingleKotlinQuickAssistProposals(editor: KotlinEditor) : List<KotlinQuickAssistProposal> {
        val quickAssists = listOf(
            KotlinReplaceGetAssistProposal(), 
            KotlinSpecifyTypeAssistProposal(),
            KotlinRemoveExplicitTypeAssistProposal(),
            KotlinImplementMethodsProposal(),
            KotlinConvertToExpressionBodyAssistProposal(),
            KotlinConvertToBlockBodyAssistProposal(),
            KotlinChangeReturnTypeProposal())
        
        for (assist in quickAssists) {
            assist.editor = editor
        }
        
        return quickAssists
    }
}