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
package org.jetbrains.kotlin.ui.editors.quickfix

import org.eclipse.core.resources.IFile
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.isOverridable
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory2
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.resolve.source.PsiSourceElement
import java.util.ArrayList

fun DiagnosticFactory<*>.createMakeDeclarationOpenFix() = MakeOverriddenMemberOpenFix()

class MakeOverriddenMemberOpenFix : KotlinDiagnosticQuickFix {

    override val handledErrors: List<DiagnosticFactory<*>>
        get() = listOf(Errors.OVERRIDING_FINAL_MEMBER)

    override fun getResolutions(diagnostic: Diagnostic): List<KotlinMarkerResolution> {
        val overrideFinalDiagnostic = Errors.OVERRIDING_FINAL_MEMBER.cast(diagnostic)
        val badOverriden: CallableMemberDescriptor = overrideFinalDiagnostic.a
        
        val firstNotOverridable: CallableMemberDescriptor? = getFirstDeclaredNonOverridableOverriddenDescriptor(badOverriden)
        if (firstNotOverridable == null) return listOf()
        
        val source = firstNotOverridable.source as? PsiSourceElement
        if (source == null) return listOf()
        
        val modifierListOwner = (source.psi as? KtModifierListOwner)
        if (modifierListOwner == null) return listOf()
        
        return listOf(KotlinAddOpenToMemberResolution(modifierListOwner, firstNotOverridable.containingDeclaration.name))
    }
    
    companion object {
        private fun getFirstDeclaredNonOverridableOverriddenDescriptor(callableMemberDescriptor: CallableMemberDescriptor): CallableMemberDescriptor? {
            if (!callableMemberDescriptor.isOverridable && callableMemberDescriptor.kind == Kind.DECLARATION) return callableMemberDescriptor
            
            val nonOverridableOverriddenDescriptors = retainNonOverridableMembers(callableMemberDescriptor.overriddenDescriptors)
            for (overriddenDescriptor in nonOverridableOverriddenDescriptors) {
                when (overriddenDescriptor.kind) {
                    Kind.DECLARATION -> return overriddenDescriptor

                    Kind.FAKE_OVERRIDE, Kind.DELEGATION ->
                        return getFirstDeclaredNonOverridableOverriddenDescriptor(overriddenDescriptor)

                    Kind.SYNTHESIZED -> {
                        /* do nothing */
                    } 

                    else -> throw UnsupportedOperationException("Unexpected callable kind ${overriddenDescriptor.kind}")
                }
            }
            
            return null
        }

        private fun retainNonOverridableMembers(callableMemberDescriptors: Collection<CallableMemberDescriptor>): Collection<CallableMemberDescriptor> {
            return callableMemberDescriptors.filter { !it.isOverridable }
        }
    }
}

class KotlinAddOpenToMemberResolution(private val element: KtModifierListOwner, private val containinName: Name) : KotlinMarkerResolution {
    override fun apply(file: IFile) {
        addModifier(element, KtTokens.OPEN_KEYWORD)
    }
    
    override fun getLabel(): String? {
        return "Make $containinName.${getElementName(element)} open"
    }
}
