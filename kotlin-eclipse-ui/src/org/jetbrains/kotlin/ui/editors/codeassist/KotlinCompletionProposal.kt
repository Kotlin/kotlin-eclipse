package org.jetbrains.kotlin.ui.editors.codeassist

import org.eclipse.jface.text.contentassist.ICompletionProposal
import org.eclipse.jface.text.IDocument
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.graphics.Image
import org.eclipse.jface.text.contentassist.IContextInformation
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import kotlin.platform.platformStatic
import org.eclipse.jface.text.contentassist.CompletionProposal

public fun withKotlinInsertHandler(
		descriptor: DeclarationDescriptor,
		proposal: KotlinCompletionProposal): ICompletionProposal {
	return when (descriptor) {
		is FunctionDescriptor -> {
			val parameters = descriptor.getValueParameters()
			when (parameters.size()) {
				0 -> KotlinFunctionCompletionProposal(proposal, CaretPosition.AFTER_BRACKETS, false)
				
				1 -> {
					val parameterType = parameters.single().getType()
					if (KotlinBuiltIns.isFunctionOrExtensionFunctionType(parameterType)) {
						val parameterCount = KotlinBuiltIns.getParameterTypeProjectionsFromFunctionType(parameterType).size()
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

public open class KotlinCompletionProposal(
		val replacementString: String,
		val replacementOffset: Int,
		val replacementLength: Int,
		val cursorPosition: Int,
		val img: Image?,
		val presentableString: String,
		val information: IContextInformation?,
		val additionalInfo: String) : ICompletionProposal {
	val defaultCompletionProposal = 
			CompletionProposal(replacementString, replacementOffset, replacementLength, cursorPosition, img, presentableString, information, additionalInfo)
	
	override fun apply(document: IDocument) {
		defaultCompletionProposal.apply(document)
	}
	
	override fun getSelection(document: IDocument): Point? = defaultCompletionProposal.getSelection(document)
	
	override fun getAdditionalProposalInfo(): String = defaultCompletionProposal.getAdditionalProposalInfo()
	
	override fun getDisplayString(): String = defaultCompletionProposal.getDisplayString()
	
	override fun getImage(): Image? = defaultCompletionProposal.getImage()
	
	override fun getContextInformation(): IContextInformation? = defaultCompletionProposal.getContextInformation()
}
