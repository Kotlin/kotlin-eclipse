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
package org.jetbrains.kotlin.ui.editors.hover

import org.eclipse.jdt.core.IJavaElement
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility
import org.eclipse.jdt.internal.ui.text.java.hover.JavadocHover
import org.eclipse.jface.internal.text.html.BrowserInformationControlInput
import org.eclipse.jface.text.Region
import org.jetbrains.kotlin.core.model.toLightElements
import org.jetbrains.kotlin.core.references.createReferences
import org.jetbrains.kotlin.core.references.getReferenceExpression
import org.jetbrains.kotlin.core.references.resolveToSourceElements
import org.jetbrains.kotlin.core.resolve.lang.java.resolver.EclipseJavaSourceElement
import org.jetbrains.kotlin.psi.KtElement

class KotlinTextHoverExtension : KotlinEditorTextHover() {
    companion object {
        private val DUMMY_REGION = Region(0, 0)
    }
    
    override fun getHoverInfo(hoverData: HoverData): BrowserInformationControlInput? {
        val (element, editor) = hoverData
        
        val javaElements = obtainJavaElements(element)
        if (javaElements.isEmpty()) return null
        
        val editorInputElement = EditorUtility.getEditorInputJavaElement(editor.javaEditor, false)
        
        return JavadocHover.getHoverInfo(javaElements.toTypedArray(), editorInputElement, DUMMY_REGION, null)
    }
    
    override fun isAvailable(hoverData: HoverData): Boolean = true
    
    private fun obtainJavaElements(element: KtElement): List<IJavaElement> {
        val expression = getReferenceExpression(element) ?: return emptyList()
        
        return createReferences(expression)
                .resolveToSourceElements()
                .filterIsInstance(EclipseJavaSourceElement::class.java)
                .flatMap { it.toLightElements() }
    }
}