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
package org.jetbrains.kotlin.ui.editors.outline

import org.eclipse.jface.viewers.ITreeContentProvider
import org.eclipse.jface.viewers.Viewer
import org.jetbrains.kotlin.psi.KtDeclarationContainer
import org.jetbrains.kotlin.psi.KtScriptInitializer

class PsiContentProvider : ITreeContentProvider {
    override fun dispose() {
    }

    override fun inputChanged(viewer: Viewer?, oldInput: Any?, newInput: Any?) {
    }

    override fun getElements(inputElement: Any?): Array<Any> = getChildren(inputElement)

    override fun getChildren(parentElement: Any?): Array<Any> {
        if (parentElement !is KtDeclarationContainer) {
            return arrayOf<Any>()
        }
        
        return parentElement
                .declarations
                .filter { it !is KtScriptInitializer }
                .toTypedArray()
    }

    override fun getParent(element: Any?): Any? = null

    override fun hasChildren(element: Any?): Boolean = getChildren(element).isNotEmpty()
}