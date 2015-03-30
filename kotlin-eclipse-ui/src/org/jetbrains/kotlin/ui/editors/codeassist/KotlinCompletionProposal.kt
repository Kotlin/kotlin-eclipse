package org.jetbrains.kotlin.ui.editors.codeassist

import org.eclipse.jface.text.contentassist.ICompletionProposal
import org.eclipse.jface.text.IDocument
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.graphics.Image
import org.eclipse.jface.text.contentassist.IContextInformation
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ui.editors.KotlinEditor
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import kotlin.platform.platformStatic

public open class KotlinCompletionProposal(val proposal: ICompletionProposal): ICompletionProposal {
	override fun apply(document: IDocument) {
		proposal.apply(document)
	}
	
	override fun getSelection(document: IDocument): Point = proposal.getSelection(document)
	
	override fun getAdditionalProposalInfo(): String = proposal.getAdditionalProposalInfo()
	
	override fun getDisplayString(): String = proposal.getDisplayString()
	
	override fun getImage(): Image = proposal.getImage()
	
	override fun getContextInformation(): IContextInformation = proposal.getContextInformation()
	
	companion object {
		platformStatic public fun getDefaultInsertHandler(
				descriptor: DeclarationDescriptor,
				proposal: ICompletionProposal,
				jetFile: JetFile,
				editor: KotlinEditor): KotlinCompletionProposal {
			return when (descriptor) {
				is FunctionDescriptor -> {
					val parameters = descriptor.getValueParameters()
							when (parameters.size()) {
						0 -> KotlinFunctionCompletionProposal(proposal, jetFile, editor, CaretPosition.AFTER_BRACKETS, null)
						
						1 -> {
							val parameterType = parameters.single().getType()
							if (KotlinBuiltIns.isFunctionOrExtensionFunctionType(parameterType)) {
								val parameterCount = KotlinBuiltIns.getParameterTypeProjectionsFromFunctionType(parameterType).size()
									if (parameterCount <= 1) {
										// otherwise additional item with lambda template is to be added
										return KotlinFunctionCompletionProposal(proposal, jetFile, editor, CaretPosition.IN_BRACKETS, GenerateLambdaInfo(parameterType, false))
									}
							}
							KotlinFunctionCompletionProposal(proposal, jetFile, editor, CaretPosition.IN_BRACKETS, null)
						}
						
						else -> KotlinFunctionCompletionProposal(proposal, jetFile, editor, CaretPosition.IN_BRACKETS, null)
					}
				}
				
				else -> KotlinCompletionProposal(proposal)
			}
		}
	}

}