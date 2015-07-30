/*******************************************************************************
 * Copyright 2000-2015 JetBrains s.r.o.
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

import org.eclipse.jface.text.contentassist.ICompletionProposal
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.DocumentEvent
import org.eclipse.jface.text.ITextViewer
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.ui.editors.completion.KotlinCompletionUtils

public abstract class KotlinHandlerCompletionProposal(
		val proposal: KotlinCompletionProposal): ICompletionProposal by proposal, ICompletionProposalExtension, ICompletionProposalExtension2 {
	
	override fun selected(viewer: ITextViewer, smartToggle: Boolean) {
	}
	
	override fun unselected(viewer: ITextViewer) {
	}
	
	override fun validate(document: IDocument, offset: Int, event: DocumentEvent): Boolean {
		val replacementLength = offset - proposal.replacementOffset
        val prefix = document.get(proposal.replacementOffset, replacementLength)
		return KotlinCompletionUtils.applicableNameFor(prefix, Name.identifier(proposal.replacementString))
	}
	
	override fun isValidFor(document: IDocument, offset: Int): Boolean {
		throw IllegalStateException("This method should never be called")
	}
	
	override fun apply(document: IDocument, trigger: Char, offset: Int) {
		throw IllegalStateException("This method should never be called")
	}
	
	override fun getContextInformationPosition(): Int = -1
}