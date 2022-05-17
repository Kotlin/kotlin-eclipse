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
package org.jetbrains.kotlin.ui.editors.navigation

import com.intellij.psi.util.PsiTreeUtil
import org.eclipse.core.commands.AbstractHandler
import org.eclipse.core.commands.ExecutionEvent
import org.eclipse.jdt.internal.ui.actions.ActionMessages
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds
import org.eclipse.jdt.ui.actions.SelectionDispatchAction
import org.eclipse.jface.text.ITextSelection
import org.eclipse.jface.viewers.ArrayContentProvider
import org.eclipse.jface.window.Window
import org.eclipse.ui.PlatformUI
import org.eclipse.ui.dialogs.ListDialog
import org.eclipse.ui.handlers.HandlerUtil
import org.jetbrains.kotlin.core.model.KotlinAnalysisFileCache
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.*
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.stubs.KotlinStubWithFqName
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.ui.editors.KotlinCommonEditor
import org.jetbrains.kotlin.ui.overrideImplement.KotlinCallableLabelProvider

class KotlinOpenSuperImplementationActionHandler : AbstractHandler() {
    override fun execute(event: ExecutionEvent): Any? {
        val editor = HandlerUtil.getActiveEditor(event)
        if (editor !is KotlinCommonEditor) return null

        KotlinOpenSuperImplementationAction(editor).run()

        return null
    }
}

class KotlinOpenAllSuperImplementationActionHandler : AbstractHandler() {
    override fun execute(event: ExecutionEvent): Any? {
        val editor = HandlerUtil.getActiveEditor(event)
        if (editor !is KotlinCommonEditor) return null

        KotlinOpenSuperImplementationAction(editor, true).run()

        return null
    }
}

class KotlinOpenSuperImplementationAction(
    private val editor: KotlinCommonEditor,
    private val recursive: Boolean = false
) : SelectionDispatchAction(editor.site) {
    init {
        actionDefinitionId = IJavaEditorActionDefinitionIds.OPEN_SUPER_IMPLEMENTATION
        text = ActionMessages.OpenSuperImplementationAction_label
        description = ActionMessages.OpenSuperImplementationAction_description
    }

    companion object {
        const val ACTION_ID = "OpenSuperImplementation"
    }

    override fun run(selection: ITextSelection) {
        val ktFile = editor.parsedFile
        val project = editor.javaProject
        if (ktFile == null || project == null) return

        val psiElement = EditorUtil.getPsiElement(editor, selection.offset) ?: return

        val declaration: KtTypeParameterListOwnerStub<out KotlinStubWithFqName<out KtTypeParameterListOwnerStub<*>>> =
            PsiTreeUtil.getParentOfType(
                psiElement,
                KtNamedFunction::class.java,
                KtClass::class.java,
                KtProperty::class.java,
                KtObjectDeclaration::class.java
            ) ?: return

        val descriptor = resolveToDescriptor(declaration)
        if (descriptor !is DeclarationDescriptor) return

        val superDeclarations =
            if (recursive) findSuperDeclarationsRecursive(descriptor) else findSuperDeclarations(descriptor)
        if (superDeclarations.isEmpty()) return

        val superDeclaration = when (superDeclarations.size) {
            1 -> superDeclarations.first()
            else -> chooseFromSelectionDialog(superDeclarations)
        } ?: return

        gotoElement(superDeclaration, declaration, editor, project)
    }

    private fun chooseFromSelectionDialog(declarations: Set<MemberDescriptor>): MemberDescriptor? {
        val shell = PlatformUI.getWorkbench().activeWorkbenchWindow.shell
        val dialog = ListDialog(shell)

        dialog.setTitle("Super Declarations")
        dialog.setMessage("Select a declaration to navigate")
        dialog.setContentProvider(ArrayContentProvider())
        dialog.setLabelProvider(KotlinCallableLabelProvider())

        dialog.setInput(declarations.toTypedArray())

        if (dialog.open() == Window.CANCEL) {
            return null
        }

        val result = dialog.result
        if (result == null || result.size != 1) {
            return null
        }

        return result[0] as MemberDescriptor
    }

    private fun resolveToDescriptor(declaration: KtDeclaration): DeclarationDescriptor? {
        val context =
            KotlinAnalysisFileCache.getAnalysisResult(declaration.containingKtFile).analysisResult.bindingContext
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

            is CallableMemberDescriptor -> descriptor.getDirectlyOverriddenDeclarations().toSet()

            else -> emptySet<MemberDescriptor>()
        }

        return superDescriptors
    }

    private fun findSuperDeclarationsRecursive(descriptor: DeclarationDescriptor): Set<MemberDescriptor> {
        val tempResult = mutableSetOf<MemberDescriptor>()
        var tempNewResults = setOf(descriptor)
        while (tempNewResults.isNotEmpty()) {
            val tempSuperResults = tempNewResults.flatMapTo(hashSetOf()) { findSuperDeclarations(it) }
            tempSuperResults -= tempResult
            tempResult += tempSuperResults
            tempNewResults = tempSuperResults
        }
        return tempResult
    }

    private fun <D : CallableMemberDescriptor> D.getDirectlyOverriddenDeclarations(): Collection<D> {
        val result = LinkedHashSet<D>()
        for (overriddenDescriptor in overriddenDescriptors) {
            @Suppress("UNCHECKED_CAST")
            when (overriddenDescriptor.kind) {
                DECLARATION -> result.add(overriddenDescriptor as D)
                FAKE_OVERRIDE, DELEGATION -> result.addAll((overriddenDescriptor as D).getDirectlyOverriddenDeclarations())
                SYNTHESIZED -> {
                    //do nothing
                }
                else -> throw AssertionError("Unexpected callable kind ${overriddenDescriptor.kind}: $overriddenDescriptor")
            }
        }
        return OverridingUtil.filterOutOverridden(result)
    }
}
