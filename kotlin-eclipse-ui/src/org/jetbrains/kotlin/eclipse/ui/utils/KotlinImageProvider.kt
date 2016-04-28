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
package org.jetbrains.kotlin.eclipse.ui.utils

import org.eclipse.jdt.ui.JavaUI
import org.eclipse.swt.graphics.Image
import org.eclipse.jdt.ui.ISharedImages
import org.jetbrains.kotlin.psi.KtVariableDeclaration
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor

public object KotlinImageProvider {
    public fun getImage(descriptor: DeclarationDescriptor): Image? {
        return when(descriptor) {
            is ClassDescriptor, is TypeParameterDescriptor -> getImageFromJavaUI(ISharedImages.IMG_OBJS_CLASS)
            is FunctionDescriptor -> getImageFromJavaUI(ISharedImages.IMG_OBJS_PUBLIC)
            is VariableDescriptor -> getImageFromJavaUI(ISharedImages.IMG_FIELD_PUBLIC)
            is PackageViewDescriptor -> getImageFromJavaUI(ISharedImages.IMG_OBJS_PACKAGE)
            is PropertyDescriptor -> getImageFromJavaUI(ISharedImages.IMG_FIELD_PUBLIC)
            else -> null
        }
    }
    
    public fun getImage(element: KtElement): Image? {
        return when(element) {
            is KtClassOrObject -> getImageFromJavaUI(ISharedImages.IMG_OBJS_CLASS)
            is KtFunction -> getImageFromJavaUI(ISharedImages.IMG_OBJS_PUBLIC)
            is KtVariableDeclaration -> getImageFromJavaUI(ISharedImages.IMG_FIELD_PUBLIC)
            else -> null
        }
    }
    
    private fun getImageFromJavaUI(imageName: String): Image = JavaUI.getSharedImages().getImage(imageName)
}