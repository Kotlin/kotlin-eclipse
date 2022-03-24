/*******************************************************************************
 * Copyright 2000-2014 JetBrains s.r.o.
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
 */
package org.jetbrains.kotlin.ui.editors

import org.eclipse.jface.text.IRegion
import org.eclipse.jface.text.hyperlink.IHyperlink
import org.jetbrains.kotlin.core.utils.getBindingContext
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.VariableDescriptorWithAccessors
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyDelegate
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.ui.editors.codeassist.getParentOfType
import org.jetbrains.kotlin.ui.editors.navigation.KotlinOpenDeclarationAction
import org.jetbrains.kotlin.ui.editors.navigation.gotoElement

class KotlinElementHyperlink(
    private val openAction: KotlinOpenDeclarationAction,
    private val region: IRegion
) : IHyperlink {
    override fun getHyperlinkRegion(): IRegion = region

    override fun getTypeLabel(): String? = null

    override fun getHyperlinkText(): String = HYPERLINK_TEXT

    override fun open() = openAction.run()
}

fun KtPropertyDelegate.doOpenDelegateFun(editor: KotlinEditor, openSetter: Boolean) {
    val property = getParentOfType<KtProperty>(false) ?: return
    val javaProject = editor.javaProject ?: return

    val context = property.getBindingContext()
    val tempDescriptor = property.resolveToDescriptorIfAny() as? VariableDescriptorWithAccessors ?: return
    val tempAccessor = if (openSetter) {
        tempDescriptor.setter
    } else {
        tempDescriptor.getter
    }
    val tempTargetDescriptor =
        context[BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, tempAccessor]?.candidateDescriptor ?: return

    gotoElement(tempTargetDescriptor, property, editor, javaProject)
}

class KTGenericHyperLink(
    private val region: IRegion,
    private val label: String,
    private val editor: KotlinEditor,
    private val targetDescriptor: DeclarationDescriptor,
    private val fromElement: KtElement
) :
    IHyperlink {
    override fun getHyperlinkRegion(): IRegion = region

    override fun getTypeLabel(): String? = null

    override fun getHyperlinkText(): String = label

    override fun open() {
        val tempPrj = editor.javaProject ?: return
        gotoElement(targetDescriptor, fromElement, editor, tempPrj)
    }

}

private const val HYPERLINK_TEXT = "Open Kotlin Declaration"
