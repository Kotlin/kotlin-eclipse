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
package org.jetbrains.kotlin.ui.search

import com.intellij.psi.util.PsiTreeUtil
import org.eclipse.jdt.ui.search.IMatchPresentation
import org.eclipse.jface.viewers.ILabelProvider
import org.eclipse.jface.viewers.LabelProvider
import org.eclipse.search.internal.ui.text.EditorOpener
import org.eclipse.search.ui.text.Match
import org.eclipse.swt.graphics.Image
import org.eclipse.ui.PlatformUI
import org.jetbrains.kotlin.core.asJava.getTypeFqName
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.jetbrains.kotlin.eclipse.ui.utils.KotlinImageProvider
import org.jetbrains.kotlin.eclipse.ui.utils.getTextDocumentOffset
import org.jetbrains.kotlin.psi.*

class KotlinReferenceMatchPresentation : IMatchPresentation {
    private val editorOpener = EditorOpener()
    
    override fun createLabelProvider(): ILabelProvider = KotlinReferenceLabelProvider()
    
    override fun showMatch(match: Match, currentOffset: Int, currentLength: Int, activate: Boolean) {
        if (match !is KotlinElementMatch) return 
        
        val identifier = match.identifier
        val element = match.jetElement
        val eclipseFile = KotlinPsiManager.getEclipseFile(element.containingKtFile)
        if (eclipseFile != null) {
            val document = EditorUtil.getDocument(eclipseFile)
            editorOpener.openAndSelect(
                PlatformUI.getWorkbench().activeWorkbenchWindow.activePage,
                eclipseFile,
                identifier.getTextDocumentOffset(document),
                identifier.textLength,
                activate
            )
        }
    }
}

class KotlinReferenceLabelProvider : LabelProvider() {
    override fun getText(element: Any): String {
        if (element !is KotlinAdaptableElement) {
            throw IllegalArgumentException("KotlinReferenceLabelProvider asked for non-reference expression: $element")
        }

        return when (val declaration = getContainingDeclaration(element.jetElement)) {
            is KtNamedDeclaration -> with (declaration) {
                fqName?.asString() ?: nameAsSafeName.asString()
            }
            is KtFile -> getTypeFqName(declaration)?.asString() ?: ""
            else -> ""
        }
    }
    
    override fun getImage(element: Any): Image? {
        val jetElement = (element as KotlinAdaptableElement).jetElement
        val containingDeclaration = getContainingDeclaration(jetElement)
        return containingDeclaration?.let { KotlinImageProvider.getImage(it) }
    }
    
    private fun getContainingDeclaration(jetElement: KtElement): KtElement? {
        return PsiTreeUtil.getNonStrictParentOfType(jetElement, 
                KtNamedFunction::class.java,
                KtProperty::class.java,
                KtClassOrObject::class.java,
                KtFile::class.java)
    }
}
