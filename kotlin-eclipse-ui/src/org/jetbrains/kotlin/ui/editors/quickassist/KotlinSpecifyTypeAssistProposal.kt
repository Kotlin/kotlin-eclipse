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
package org.jetbrains.kotlin.ui.editors.quickassist

import com.intellij.psi.PsiElement
import org.eclipse.jface.text.IDocument
import org.jetbrains.kotlin.diagnostics.Errors
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.JetCallableDeclaration
import org.jetbrains.kotlin.psi.JetCodeFragment
import org.jetbrains.kotlin.psi.JetFunctionLiteral
import org.jetbrains.kotlin.psi.JetConstructor
import org.jetbrains.kotlin.psi.JetFunction
import org.jetbrains.kotlin.psi.JetNamedFunction
import org.jetbrains.kotlin.psi.JetWithExpressionInitializer
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer
import org.eclipse.jdt.core.JavaCore
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.types.ErrorUtils
import kotlin.properties.Delegates
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.JetProperty
import org.jetbrains.kotlin.psi.JetParameter
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor

public class KotlinSpecifyTypeAssistProposal : KotlinQuickAssistProposal() {
    private var displayString: String? = null
    
    override fun isApplicable(psiElement: PsiElement): Boolean {
        val element = PsiTreeUtil.getNonStrictParentOfType(psiElement, JetCallableDeclaration::class.java)
        if (element == null) return false
        
        if (element.getContainingFile() is JetCodeFragment) return false
        if (element is JetFunctionLiteral) return false
        if (element is JetConstructor<*>) return false
        if (element.getTypeReference() != null) return false
        
        val editor = getActiveEditor()
        if (editor == null) return false
        val caretOffset = getCaretOffsetInPSI(editor, editor.document)
        
        val initializer = (element as? JetWithExpressionInitializer)?.getInitializer()
        if (initializer != null && initializer.getTextRange().containsOffset(caretOffset)) return false

        if (element is JetNamedFunction && element.hasBlockBody()) return false
        
        if (getTypeForDeclaration(element).isError()) return false

        displayString = if (element is JetFunction) "Specify return type explicitly" else "Specify type explicitly"
        
        return true
    }
    
    override fun getDisplayString(): String? {
        return displayString
    }
    
    override fun apply(document: IDocument, psiElement: PsiElement) {
        val element = PsiTreeUtil.getNonStrictParentOfType(psiElement, JetCallableDeclaration::class.java)!!
        val type = getTypeForDeclaration(element)
        val anchor = getAnchor(element)
        
        if (anchor != null) {
            val editor = getActiveEditor()
            if (editor == null) return
            
            val offset = addTypeAnnotation(editor, document, anchor, type)
            editor.getViewer().setSelectedRange(offset, 0)
        }
    }
    
    private fun addTypeAnnotation(editor: KotlinFileEditor, document: IDocument, element: PsiElement, type: JetType): Int {
        val offset = getEndOffset(element, editor)
        val text = ": ${IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(type)}"
        document.replace(getEndOffset(element, editor), 0, text)
        
        return offset + text.length()
    }
    
    private fun getTypeForDeclaration(declaration: JetCallableDeclaration): JetType {
        val bindingContext = getBindingContext(declaration.getContainingJetFile())
        if (bindingContext == null) return ErrorUtils.createErrorType("null type")
        
        val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration]
        val type = (descriptor as? CallableDescriptor)?.getReturnType()
        return type ?: ErrorUtils.createErrorType("null type")
    }
}

fun getAnchor(element: JetCallableDeclaration): PsiElement? {
    return when (element){
        is JetProperty -> element.getNameIdentifier()
        is JetNamedFunction -> element.getValueParameterList()
        is JetParameter -> element.getNameIdentifier()
        else -> null
    }
}