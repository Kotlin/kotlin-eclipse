package org.jetbrains.kotlin.ui.tests.editors.completion

import org.eclipse.jface.text.contentassist.ContentAssistant
import org.eclipse.jface.text.contentassist.ICompletionProposal
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.jetbrains.kotlin.ui.editors.codeassist.KotlinCompletionProcessor

fun getCompletionProposals(editor: KotlinFileEditor): Array<ICompletionProposal> {
    val processor = KotlinCompletionProcessor(editor, ContentAssistant())
    return processor.computeCompletionProposals(editor.getViewer(), KotlinTestUtils.getCaret(editor))
}