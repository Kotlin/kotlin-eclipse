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
package org.jetbrains.kotlin.ui.editors.hover

import org.eclipse.jface.text.information.IInformationProvider
import org.eclipse.jface.text.information.IInformationProviderExtension
import org.eclipse.jface.text.information.IInformationProviderExtension2
import org.eclipse.jface.text.ITextViewer
import org.eclipse.jface.text.IRegion
import org.eclipse.jface.text.IInformationControlCreator
import org.eclipse.jdt.internal.ui.text.JavaWordFinder
import org.eclipse.jdt.internal.ui.text.java.hover.JavadocBrowserInformationControlInput
import org.jetbrains.kotlin.ui.editors.KotlinEditor
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.jetbrains.kotlin.core.references.resolveToSourceDeclaration
import org.jetbrains.kotlin.core.resolve.lang.java.resolver.EclipseJavaSourceElement
import org.eclipse.jdt.internal.ui.text.java.hover.JavadocHover

public class KotlinInformationProvider(val editor: KotlinEditor) : IInformationProvider, IInformationProviderExtension, IInformationProviderExtension2 {
    override fun getSubject(textViewer: ITextViewer?, offset: Int): IRegion? {
        return if (textViewer != null) JavaWordFinder.findWord(textViewer.getDocument(), offset) else null
    }
    
    override fun getInformation(textViewer: ITextViewer?, subject: IRegion?): String? = null // deprecated method
    
    override fun getInformation2(textViewer: ITextViewer?, subject: IRegion): Any? {
        val jetElement = EditorUtil.getJetElement(editor, subject.getOffset())
        if (jetElement == null) return null
        
        val sourceElements = jetElement.resolveToSourceDeclaration()
        if (sourceElements.isEmpty()) return null
        
        val javaElements = sourceElements.filterIsInstance(EclipseJavaSourceElement::class.java)
        if (javaElements.isNotEmpty()) {
            val elements = javaElements.mapNotNull { it.getElementBinding().getJavaElement() }
            return JavadocHover.getHoverInfo(elements.toTypedArray(), null, subject, null)
        }
        
        return null
    }
    
    override fun getInformationPresenterControlCreator(): IInformationControlCreator {
        return JavadocHover.PresenterControlCreator(editor.javaEditor.getSite())
    }
}