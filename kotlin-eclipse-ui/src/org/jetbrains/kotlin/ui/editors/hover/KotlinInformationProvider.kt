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
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.NameShortness
import org.jetbrains.kotlin.core.references.getReferenceExpression
import org.jetbrains.kotlin.core.references.createReferences
import org.jetbrains.kotlin.ui.editors.getKotlinDeclaration
import org.jetbrains.kotlin.core.model.KotlinAnalysisFileCache
import org.jetbrains.kotlin.core.references.KotlinReference
import org.eclipse.jface.internal.text.html.HTMLPrinter

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
        
        val kotlinElements = sourceElements - javaElements
        if (kotlinElements.isNotEmpty()) {
            val referenceExpression = getReferenceExpression(jetElement)
            if (referenceExpression == null) return null
            
            val reference = createReferences(referenceExpression).first()
            val kotlinDeclaration = getKotlinDeclaration(kotlinElements.first(), reference, editor.javaProject!!)
            if (kotlinDeclaration is KtDeclaration) {
                val context = KotlinAnalysisFileCache.getAnalysisResult(editor.parsedFile!!, editor.javaProject!!).analysisResult.bindingContext
                return renderKotlinDeclaration(kotlinDeclaration, context, reference)
            }
        }
        
        return null
    }
    
    override fun getInformationPresenterControlCreator(): IInformationControlCreator {
        return JavadocHover.PresenterControlCreator(editor.javaEditor.getSite())
    }
}

private val DESCRIPTOR_RENDERER = DescriptorRenderer.HTML.withOptions {
    nameShortness = NameShortness.SHORT
    renderCompanionObjectName = true
}

private fun renderKotlinDeclaration(declaration: KtDeclaration, context: BindingContext, reference: KotlinReference): String {
    val declarationDescriptor = reference.getTargetDescriptors(context).firstOrNull()

    if (declarationDescriptor == null) {
        return "No documentation available"
    }

    var renderedDecl = DESCRIPTOR_RENDERER.render(declarationDescriptor)
    
    val comment = findKDoc(declarationDescriptor, declaration)
    if (comment != null) {
        val renderedComment = KDocRenderer.renderKDoc(comment)
        if (renderedComment.startsWith("<p>")) {
            renderedDecl += renderedComment
        }
        else {
            renderedDecl = "$renderedDecl<br/>$renderedComment"
        }
    }
    
    return with(StringBuffer(renderedDecl)) { 
        HTMLPrinter.insertPageProlog(this, 0, getStyleSheet())
        HTMLPrinter.addPageEpilog(this)
        
        toString()
    }
}

private fun getStyleSheet(): String {
    val method = JavadocHover::class.java.getDeclaredMethod("getStyleSheet")
    method.setAccessible(true)
    return method.invoke(null) as String
}
