package org.jetbrains.kotlin.ui.editors.quickassist

import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import com.intellij.psi.PsiElement

public class KotlinTypeMismatchQuickFixGenerator : KotlinQuickAssistProposalsGenerator() {
    override fun getProposals(kotlinFileEditor: KotlinFileEditor, psiElement: PsiElement): List<KotlinQuickAssistProposal> {
        throw UnsupportedOperationException()
    }
    
    override fun isApplicable(psiElement: PsiElement): Boolean {
        throw UnsupportedOperationException()
    }
}