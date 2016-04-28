/*******************************************************************************
* Copyright 2000-2016 JetBrains s.r.o.
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
import org.eclipse.jface.text.IDocument
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.graphics.Image
import org.eclipse.jface.text.contentassist.IContextInformation
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.eclipse.jface.text.contentassist.CompletionProposal
import org.eclipse.jface.viewers.StyledString
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension6

public fun withKotlinInsertHandler(
        descriptor: DeclarationDescriptor,
        proposal: KotlinCompletionProposal): ICompletionProposal {
    return when (descriptor) {
        is FunctionDescriptor -> {
            val parameters = descriptor.getValueParameters()
            when (parameters.size) {
                0 -> KotlinFunctionCompletionProposal(proposal, CaretPosition.AFTER_BRACKETS, false)

                1 -> {
                    val parameterType = parameters.single().getType()
                    if (KotlinBuiltIns.isFunctionOrExtensionFunctionType(parameterType)) {
                        val parameterCount = KotlinBuiltIns.getParameterTypeProjectionsFromFunctionType(parameterType).size
                        if (parameterCount <= 1) {
                            // otherwise additional item with lambda template is to be added
                            return KotlinFunctionCompletionProposal(proposal, CaretPosition.IN_BRACKETS, true)
                        }
                    }
                    KotlinFunctionCompletionProposal(proposal, CaretPosition.IN_BRACKETS, false)
                }

                else -> KotlinFunctionCompletionProposal(proposal, CaretPosition.IN_BRACKETS, false)
            }
        }

        else -> proposal
    }
}

open class KotlinCompletionProposal(
        val replacementString: String,
        val replacementOffset: Int,
        replacementLength: Int,
        cursorPosition: Int,
        img: Image,
        presentableString: String,
        information: IContextInformation? = null,
        additionalInfo: String? = null,
        val styledPresentableString: StyledString? = null) : ICompletionProposal, ICompletionProposalExtension6 {
    
    val defaultCompletionProposal =
            CompletionProposal(
                    replacementString, 
                    replacementOffset, 
                    replacementLength, 
                    cursorPosition, 
                    img, 
                    presentableString, 
                    information, 
                    additionalInfo ?: replacementString)

    override fun apply(document: IDocument) {
        defaultCompletionProposal.apply(document)
    }

    override fun getSelection(document: IDocument): Point = defaultCompletionProposal.getSelection(document)

    override fun getAdditionalProposalInfo(): String = defaultCompletionProposal.getAdditionalProposalInfo()

    override fun getDisplayString(): String = defaultCompletionProposal.getDisplayString()

    override fun getImage(): Image? = defaultCompletionProposal.getImage()

    override fun getContextInformation(): IContextInformation? = defaultCompletionProposal.getContextInformation()
    
    override fun getStyledDisplayString(): StyledString = styledPresentableString ?: StyledString(defaultCompletionProposal.displayString)
}
