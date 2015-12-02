package org.jetbrains.kotlin.ui.editors.quickassist

import com.intellij.psi.PsiElement
import org.eclipse.jface.text.IDocument
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.builtins.KotlinBuiltIns

class KotlinChangeReturnTypeProposal(val function: KtFunction, val type: KotlinType) : KotlinQuickAssistProposal() {
    override fun apply(document: IDocument, psiElement: PsiElement) {
        val oldTypeRef = function.getTypeReference()
        val renderedType = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(type)
        if (oldTypeRef != null) {
            if (KotlinBuiltIns.isUnit(type)) {
                replace(oldTypeRef, "")
            } else {
                replace(oldTypeRef, renderedType)
            }
        } else {
            val anchor = function.getValueParameterList()
            if (anchor != null) {
                insertAfter(anchor, ": $renderedType")
            }
        }
    }
    
    override fun getDisplayString(): String {
        val functionName = function.getName() ?: ""
        val renderedType = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(type)
        return "Change $functionName function return type to $renderedType"
    }
    
    override fun isApplicable(psiElement: PsiElement): Boolean = true
}