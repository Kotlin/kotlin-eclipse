package org.jetbrains.kotlin.ui.editors.quickfix

import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.eclipse.core.resources.IFile
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.ui.editors.quickassist.remove
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Diagnostic
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.PsiWhiteSpace

fun DiagnosticFactory<*>.createRemoveModifierFromListOwnerFactory(
        modifier: KtModifierKeywordToken,
        isRedundant: Boolean = false): KotlinDiagnosticQuickFix {
    return object : KotlinDiagnosticQuickFix {
        override fun getResolutions(diagnostic: Diagnostic): List<KotlinMarkerResolution> {
            val modifierListOwner = PsiTreeUtil.getParentOfType(diagnostic.psiElement, KtModifierListOwner::class.java, false)
            if (modifierListOwner == null) return emptyList()
            
            return listOf(KotlinRemoveModifierResolution(modifierListOwner, modifier, isRedundant))
        }
        
        override val handledErrors: List<DiagnosticFactory<*>>
            get() = listOf(this@createRemoveModifierFromListOwnerFactory)
    }
}

fun DiagnosticFactory<*>.createRemoveModifierFactory(isRedundant: Boolean = false): KotlinDiagnosticQuickFix {
    return object : KotlinDiagnosticQuickFix {
        override fun getResolutions(diagnostic: Diagnostic): List<KotlinMarkerResolution> {
            val psiElement = diagnostic.psiElement
            val elementType = psiElement.node.elementType as? KtModifierKeywordToken ?: return emptyList()
            
            val modifierListOwner = PsiTreeUtil.getParentOfType(psiElement, KtModifierListOwner::class.java) ?: return emptyList()
            return listOf(KotlinRemoveModifierResolution(modifierListOwner, elementType, isRedundant))
            
        }
        
        override val handledErrors: List<DiagnosticFactory<*>>
            get() = listOf(this@createRemoveModifierFactory)
    }
}

class KotlinRemoveModifierResolution(
        private val element: KtModifierListOwner,
        private val modifier: KtModifierKeywordToken,
        private val isRedundant: Boolean) : KotlinMarkerResolution {
    
    override fun apply(file: IFile) {
        removeModifier(element, modifier)
    }
    
    override fun getLabel(): String {
        val modifierText = modifier.value
        return when {
            isRedundant -> "Remove redundant '$modifierText' modifier"
            
            modifier === KtTokens.ABSTRACT_KEYWORD || modifier === KtTokens.OPEN_KEYWORD ->
                "Make '${getElementName(element)}' not $modifierText"
            
            else -> "Remove '$modifierText' modifier"
        }
    }
}

private fun removeModifier(owner: KtModifierListOwner, modifier: KtModifierKeywordToken) {
    val document = openEditorAndGetDocument(owner)
    if (document == null) return
    
    val modifierList = owner.modifierList
    if (modifierList == null) return
    
    modifierList.getModifier(modifier)?.let { m ->
        val sibling = m.nextSibling
        if (sibling is PsiWhiteSpace) {
            remove(sibling, document)
        } else if (sibling == null) {
            val modifiersSibling = modifierList.nextSibling
            if (modifiersSibling is PsiWhiteSpace) {
                remove(modifiersSibling, document)
            }
        }
        
        remove(m, document)
    }
}