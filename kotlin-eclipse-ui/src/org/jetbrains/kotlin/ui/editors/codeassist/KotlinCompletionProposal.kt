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

public fun withKotlinInsertHandler(
		descriptor: DeclarationDescriptor,
		proposal: ICompletionProposal): KotlinCompletionProposal {
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
		
		else -> KotlinCompletionProposal(proposal)
	}
}

public open class KotlinCompletionProposal(val proposal: ICompletionProposal) : ICompletionProposal by proposal