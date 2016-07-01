package org.jetbrains.kotlin.ui.tests.editors.completion

import org.eclipse.jface.text.contentassist.ICompletionProposal
import org.jetbrains.kotlin.ui.editors.KotlinEditor
import org.jetbrains.kotlin.ui.editors.codeassist.KotlinKeywordCompletionProposal

abstract class KotlinKeywordCompletionTestCase : KotlinBasicCompletionTestCase() {
    override fun getApplicableProposals(editor: KotlinEditor): Array<ICompletionProposal> {
        return getCompletionProposals(editor)
                .filterIsInstance(KotlinKeywordCompletionProposal::class.java)
                .toTypedArray()
    }
}