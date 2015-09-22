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

import org.eclipse.jface.viewers.LabelProvider
import org.eclipse.jface.viewers.ITreeContentProvider
import org.eclipse.jface.viewers.Viewer
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.NameShortness
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.eclipse.swt.graphics.Image
import org.jetbrains.kotlin.ui.editors.completion.KotlinCompletionUtils
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.eclipse.ui.utils.KotlinImageProvider

class KotlinCallableLabelProvider : LabelProvider() {
    private val MEMBER_RENDERER = DescriptorRenderer.withOptions { 
        withDefinedIn = false
        modifiers = emptySet()
        startFromName = true
        nameShortness = NameShortness.SHORT
    }
    
    override fun getText(element: Any): String {
        return when (element) {
            is ClassDescriptor -> DescriptorUtils.getFqNameSafe(element).render()
            is CallableMemberDescriptor -> MEMBER_RENDERER.render(element)
            else -> throw IllegalArgumentException("$element occurred in override/implement dialog")
        }
    }
    
    override fun getImage(element: Any): Image? {
        return if (element is DeclarationDescriptor) KotlinImageProvider.getImage(element) else null
    }
}

class KotlinCallableContentProvider(val descriptors: Set<CallableMemberDescriptor>, val types: Set<ClassDescriptor>) : ITreeContentProvider {
    override fun dispose() {
    }
    
    override fun inputChanged(viewer: Viewer?, oldInput: Any?, newInput: Any?) {
    }
    
    override fun getParent(element: Any): Any? {
        return null
    }
    
    override fun getElements(inputElement: Any?): Array<out Any>? {
        return types.toTypedArray()
    }
    
    override fun hasChildren(element: Any?): Boolean {
        return element is ClassDescriptor
    }
    
    override fun getChildren(parentElement: Any): Array<out Any>? {
        return if (parentElement is ClassDescriptor) {
            descriptors.filter { it.getContainingDeclaration() == parentElement }.toTypedArray()
        } else {
            return null
        }
    }
}