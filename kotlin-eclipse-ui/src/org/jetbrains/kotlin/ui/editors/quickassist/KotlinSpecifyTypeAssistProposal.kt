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
package org.jetbrains.kotlin.ui.editors.quickassist

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.eclipse.jface.text.IDocument
import org.jetbrains.kotlin.core.utils.getBindingContext
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.error.ErrorTypeKind
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.ui.editors.KotlinEditor

class KotlinSpecifyTypeAssistProposal(editor: KotlinEditor) : KotlinQuickAssistProposal(editor) {
    private var displayString: String? = null
    
    override fun isApplicable(psiElement: PsiElement): Boolean {
        val element =
            PsiTreeUtil.getNonStrictParentOfType(psiElement, KtCallableDeclaration::class.java) ?: return false
        
        if (element.containingFile is KtCodeFragment) return false
        if (element is KtFunctionLiteral) return false
        if (element is KtConstructor<*>) return false
        if (element.typeReference != null) return false
        
        val caretOffset = getCaretOffsetInPSI(editor, editor.document)
        
        val initializer = (element as? KtDeclarationWithInitializer)?.initializer
        if (initializer != null && initializer.textRange.containsOffset(caretOffset)) return false

        if (element is KtNamedFunction && element.hasBlockBody()) return false
        
        if (getTypeForDeclaration(element).isError) return false

        displayString = if (element is KtFunction) "Specify return type explicitly" else "Specify type explicitly"
        
        return true
    }
    
    override fun getDisplayString(): String = displayString ?: ""
    
    override fun apply(document: IDocument, psiElement: PsiElement) {
        val element = PsiTreeUtil.getNonStrictParentOfType(psiElement, KtCallableDeclaration::class.java)!!
        val type = getTypeForDeclaration(element)
        val anchor = getAnchor(element)
        
        if (anchor != null) {
            val offset = addTypeAnnotation(editor, document, anchor, type)
            editor.javaEditor.viewer.setSelectedRange(offset, 0)
        }
    }
    
    private fun addTypeAnnotation(editor: KotlinEditor, document: IDocument, element: PsiElement, type: KotlinType): Int {
        val offset = getEndOffset(element, editor)
        val text = ": ${IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.renderType(type)}"
        document.replace(getEndOffset(element, editor), 0, text)
        
        return offset + text.length
    }
    
    private fun getTypeForDeclaration(declaration: KtCallableDeclaration): KotlinType {
        val bindingContext = declaration.getBindingContext()
        
        val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration]
        val type = (descriptor as? CallableDescriptor)?.returnType
        return type ?: ErrorUtils.createErrorType(ErrorTypeKind.UNRESOLVED_TYPE)
    }
}

fun getAnchor(element: KtCallableDeclaration): PsiElement? {
    return when (element){
        is KtProperty -> element.getNameIdentifier()
        is KtNamedFunction -> element.getValueParameterList()
        is KtParameter -> element.getNameIdentifier()
        else -> null
    }
}