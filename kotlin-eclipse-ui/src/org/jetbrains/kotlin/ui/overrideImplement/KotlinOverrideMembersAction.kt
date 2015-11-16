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
package org.jetbrains.kotlin.ui.overrideImplement

import org.eclipse.jdt.ui.actions.SelectionDispatchAction
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.eclipse.jface.text.ITextSelection
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds
import org.eclipse.jdt.internal.ui.actions.ActionMessages
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds
import org.eclipse.ui.PlatformUI
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import java.util.LinkedHashSet
import org.jetbrains.kotlin.descriptors.Modality
import java.util.LinkedHashMap
import java.util.ArrayList
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ui.editors.quickassist.resolveToDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import com.intellij.util.SmartList
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import com.intellij.psi.util.PsiTreeUtil
import org.eclipse.ui.dialogs.CheckedTreeSelectionDialog
import org.eclipse.jface.window.Window
import org.jetbrains.kotlin.ui.editors.quickassist.KotlinImplementMethodsProposal

public class KotlinOverrideMembersAction(
        val editor: KotlinFileEditor, 
        val filterMembersFromAny: Boolean = false) : SelectionDispatchAction(editor.getSite()) {
    init {
        setActionDefinitionId(IJavaEditorActionDefinitionIds.OVERRIDE_METHODS)
        PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.ADD_UNIMPLEMENTED_METHODS_ACTION)
        
        val overrideImplementText = "Override/Implement Members"
        setText(overrideImplementText)
        setDescription("Override or implement members declared in supertypes")
        setToolTipText(overrideImplementText)
        
    }
    
    val membersFromAny = listOf("equals", "hashCode", "toString")
    
    companion object {
        val ACTION_ID = "OverrideMethods"
    }
    
    override fun run(selection: ITextSelection) {
        val jetClassOrObject = getKtClassOrObject(selection)
        if (jetClassOrObject == null) return
        
        val generatedMembers = collectMembersToGenerate(jetClassOrObject)
        val selectedMembers = if (filterMembersFromAny) {
            generatedMembers.filterNot { it.getName().asString() in membersFromAny }
        } else {
            showDialog(generatedMembers)
        }
        
        if (selectedMembers.isEmpty()) return
        
        KotlinImplementMethodsProposal().generateMethods(editor.document, jetClassOrObject, selectedMembers.toSet())
    }
    
    private fun getKtClassOrObject(selection: ITextSelection): KtClassOrObject? {
        val psiElement = EditorUtil.getPsiElement(editor, selection.getOffset())
        return if (psiElement != null) {
                PsiTreeUtil.getNonStrictParentOfType(psiElement, KtClassOrObject::class.java)
            } else {
                null
            }
    }
    
    private fun showDialog(descriptors: Set<CallableMemberDescriptor>): Set<CallableMemberDescriptor> {
        val shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell()
        
        val types = descriptors.map { it.getContainingDeclaration() as ClassDescriptor }.toSet()
        val dialog = CheckedTreeSelectionDialog(shell,
                KotlinCallableLabelProvider(),
                KotlinCallableContentProvider(descriptors, types))
        
        dialog.setTitle("Override/Implement members")
        dialog.setContainerMode(true)
        dialog.setExpandedElements(types.toTypedArray())
        dialog.setInput(Any()) // Initialize input
        
        if (dialog.open() != Window.OK) {
            return emptySet()
        }
        
        val selected = dialog.getResult()?.filterIsInstance(CallableMemberDescriptor::class.java)
        return selected?.toSet() ?: emptySet()
    }
    
    private fun collectMembersToGenerate(classOrObject: KtClassOrObject): Set<CallableMemberDescriptor> {
        val descriptor = classOrObject.resolveToDescriptor()
        if (descriptor !is ClassDescriptor) return emptySet()
        
        val result = LinkedHashSet<CallableMemberDescriptor>()
        for (member in descriptor.unsubstitutedMemberScope.getContributedDescriptors()) {
            if (member is CallableMemberDescriptor
                && (member.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE || member.kind == CallableMemberDescriptor.Kind.DELEGATION)) {
                val overridden = member.overriddenDescriptors
                if (overridden.any { it.modality == Modality.FINAL || Visibilities.isPrivate(it.visibility.normalize()) }) continue

                class Data(
                        val realSuper: CallableMemberDescriptor,
                        val immediateSupers: MutableList<CallableMemberDescriptor> = SmartList()
                )

                val byOriginalRealSupers = LinkedHashMap<CallableMemberDescriptor, Data>()
                for (immediateSuper in overridden) {
                    for (realSuper in toRealSupers(immediateSuper)) {
                        byOriginalRealSupers.getOrPut(realSuper.original) { Data(realSuper) }.immediateSupers.add(immediateSuper)
                    }
                }

                val realSupers = byOriginalRealSupers.values.map { it.realSuper }
                val nonAbstractRealSupers = realSupers.filter { it.modality != Modality.ABSTRACT }
                val realSupersToUse = if (nonAbstractRealSupers.isNotEmpty()) {
                    nonAbstractRealSupers
                }
                else {
                    listOf(realSupers.first())
                }

                for (realSuper in realSupersToUse) {
                    val immediateSupers = byOriginalRealSupers[realSuper.original]!!.immediateSupers
                    assert(immediateSupers.isNotEmpty())

                    val immediateSuperToUse = if (immediateSupers.size == 1) {
                        immediateSupers.single()
                    }
                    else {
                        immediateSupers.singleOrNull { (it.containingDeclaration as? ClassDescriptor)?.kind == ClassKind.CLASS } ?: immediateSupers.first()
                    }

                    result.add(immediateSuperToUse)
                }
            }
        }
        
        return result
    }
    
    private fun toRealSupers(immediateSuper: CallableMemberDescriptor): Collection<CallableMemberDescriptor> {
        if (immediateSuper.kind != CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
            return listOf(immediateSuper)
        }

        val overridden = immediateSuper.overriddenDescriptors
        assert(overridden.isNotEmpty())
        return overridden.flatMap { toRealSupers(it) }.distinctBy { it.original }
    }
}