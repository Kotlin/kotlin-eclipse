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
package org.jetbrains.kotlin.ui.editors

import org.eclipse.jdt.ui.actions.SelectionDispatchAction
import org.eclipse.jdt.internal.ui.actions.ActionMessages
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds
import org.eclipse.jface.text.ITextSelection
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.core.model.KotlinAnalysisFileCache
import org.eclipse.jdt.core.IJavaProject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.resolve.OverrideResolver
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.core.resolve.EclipseDescriptorUtils
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.MemberDescriptor

public class KotlinOpenSuperImplementationAction(val editor: KotlinFileEditor) : SelectionDispatchAction(editor.site) {
    init {
        setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_SUPER_IMPLEMENTATION)
        setText(ActionMessages.OpenSuperImplementationAction_label)
        setDescription(ActionMessages.OpenSuperImplementationAction_description)
    }
    
    companion object {
        val ACTION_ID = "OpenSuperImplementation"
    }
    
    override fun run(selection: ITextSelection) {
        val ktFile = editor.parsedFile
        val project = editor.javaProject
        if (ktFile == null || project == null) return
        
        val psiElement = EditorUtil.getPsiElement(editor, selection.offset)
        if (psiElement == null) return
        
        val declaration: KtDeclaration? = PsiTreeUtil.getParentOfType(psiElement, 
                KtNamedFunction::class.java,
                KtClass::class.java,
                KtProperty::class.java,
                KtObjectDeclaration::class.java)
        if (declaration == null) return
        
        val descriptor = resolveToDescriptor(declaration, project)
        if (descriptor !is DeclarationDescriptor) return
        
        val superDeclarations = findSuperDeclarations(descriptor)
        if (superDeclarations.isEmpty()) return
        
        gotoElement(superDeclarations.first(), declaration, editor, project)
    }
    
    private fun resolveToDescriptor(declaration: KtDeclaration, project: IJavaProject): DeclarationDescriptor? {
        val context = KotlinAnalysisFileCache.getAnalysisResult(declaration.getContainingKtFile(), project).analysisResult.bindingContext
        return context[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration]
    }
    
    private fun findSuperDeclarations(descriptor: DeclarationDescriptor): Set<MemberDescriptor> {
        val superDescriptors = when (descriptor) {
            is ClassDescriptor -> {
                descriptor.typeConstructor.supertypes.mapNotNull { 
                    val declarationDescriptor = it.constructor.declarationDescriptor
                    if (declarationDescriptor is ClassDescriptor) declarationDescriptor else null
                }.toSet()
            }
            
            is CallableMemberDescriptor -> OverrideResolver.getDirectlyOverriddenDeclarations(descriptor).toSet()
            
            else -> emptySet<MemberDescriptor>()
        }
        
        return superDescriptors
    }
}