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
package org.jetbrains.kotlin.preferences

import org.eclipse.jface.viewers.ITreeContentProvider
import org.eclipse.jface.viewers.LabelProvider
import org.eclipse.jface.viewers.Viewer
import org.jetbrains.kotlin.ui.editors.highlighting.KotlinSyntaxCategory
import org.jetbrains.kotlin.ui.editors.highlighting.KotlinHighlightingAttributes

public class KotlinSyntaxColoringProvider(val categories: List<KotlinSyntaxCategory>) : LabelProvider(), ITreeContentProvider {
    override fun getText(element: Any): String {
        return when (element) {
            is KotlinSyntaxCategory -> element.name
            is KotlinHighlightingAttributes -> element.name
            else -> throw IllegalStateException("Cannot get name by $element")
        }
    }

    override fun inputChanged(viewer: Viewer?, oldInput: Any?, newInput: Any?) {
    }
    
    override fun getParent(element: Any?): Any? = categories.find { element in it.elements }
    
    override fun getElements(inputElement: Any): Array<out Any> = categories.toTypedArray()
    
    override fun hasChildren(element: Any): Boolean = getChildren(element).isNotEmpty()
    
    override fun getChildren(parentElement: Any): Array<out Any> {
        return when(parentElement) {
            is KotlinSyntaxCategory -> parentElement.elements.toTypedArray()
            else -> emptyArray<Any>()
        }
    }
}