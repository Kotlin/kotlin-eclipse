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
package org.jetbrains.kotlin.ui.editors.quickfix

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.eclipse.jface.text.IDocument
import org.eclipse.jdt.core.IType
import org.eclipse.jdt.core.search.SearchEngine
import org.eclipse.jdt.core.search.SearchPattern
import org.eclipse.jdt.core.search.IJavaSearchConstants
import org.eclipse.jdt.core.search.TypeNameMatchRequestor
import org.eclipse.jdt.core.search.TypeNameMatch
import org.eclipse.jdt.core.Flags
import org.eclipse.swt.graphics.Image
import org.eclipse.core.resources.IFile
import org.eclipse.jdt.ui.JavaUI
import org.eclipse.jdt.ui.ISharedImages
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtImportList
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.eclipse.ui.utils.IndenterUtil
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.eclipse.jface.text.TextUtilities
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.jetbrains.kotlin.eclipse.ui.utils.getEndLfOffset

object KotlinAutoImportQuickFix : KotlinDiagnosticQuickFix {
    override fun getResolutions(diagnostic: Diagnostic): List<KotlinMarkerResolution> {
        val typeName = diagnostic.psiElement.text
        return findApplicableTypes(typeName).map { KotlinAutoImportResolution(it) }
    }

    override fun canFix(diagnostic: Diagnostic): Boolean {
        return diagnostic.factory == Errors.UNRESOLVED_REFERENCE
    }
    
    private fun findApplicableTypes(typeName: String): List<IType> {
        val scope = SearchEngine.createWorkspaceScope()
        
        val foundTypes = arrayListOf<IType>()
        val collector = object : TypeNameMatchRequestor() {
            override fun acceptTypeNameMatch(match: TypeNameMatch) {
                val type = match.type
                if (Flags.isPublic(type.flags)) {
                    foundTypes.add(type)
                }
            }
        }
        
        val searchEngine = SearchEngine()
        searchEngine.searchAllTypeNames(null, 
                    SearchPattern.R_EXACT_MATCH, 
                    typeName.toCharArray(), 
                    SearchPattern.R_EXACT_MATCH, 
                    IJavaSearchConstants.TYPE, 
                    scope, 
                    collector,
                    IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, 
                    null)
        
        return foundTypes
    }
}

class KotlinAutoImportResolution(private val type: IType): KotlinFileMarkerResolution() {
    override fun apply(file: IFile) {
        val placeElement = findNodeToNewImport(file)
        if (placeElement == null) return
        
        val breakLineBefore = computeBreakLineBeforeImport(placeElement)
        val breakLineAfter = computeBreakLineAfterImport(placeElement)
        
        val document = EditorUtil.getDocument(file)
        val lineDelimiter = TextUtilities.getDefaultLineDelimiter(document)
        
        val newImport = "${IndenterUtil.createWhiteSpace(0, breakLineBefore, lineDelimiter)}import ${getFqName()}" +
                "${IndenterUtil.createWhiteSpace(0, breakLineAfter, lineDelimiter)}"
        
        document.replace(placeElement.getEndLfOffset(document), 0, newImport)
    }

    override fun getLabel(): String? = "Import '${type.elementName}' (${type.packageFragment.elementName})"
    
    override fun getImage(): Image? = JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_IMPDECL)
    
    override fun getDescription(): String? = null
    
    private fun getFqName(): String = type.getFullyQualifiedName('.')
}

private fun computeBreakLineAfterImport(element: PsiElement): Int {
    if (element is KtPackageDirective) {
        val nextSibling = element.getNextSibling()
        if (nextSibling is KtImportList) {
            val importList = nextSibling
            if (importList.getImports().isNotEmpty()) {
                return 2
            } else {
                return countBreakLineAfterImportList(nextSibling.getNextSibling())
            }
        }
    }
    
    return 0
}

private fun countBreakLineAfterImportList(psiElement: PsiElement):Int {
    if (psiElement is PsiWhiteSpace) {
        val countBreakLineAfterHeader = IndenterUtil.getLineSeparatorsOccurences(psiElement.getText())
        return when (countBreakLineAfterHeader) {
            0 -> 2
            1 -> 1
            else -> 0
        }
    }
    
    return 2
}

private fun computeBreakLineBeforeImport(element:PsiElement):Int {
    if (element is KtPackageDirective) {
        return when {
            element.isRoot() -> 0
            else -> 2
        } 
    }
    
    return 1
}

private fun findNodeToNewImport(file: IFile): PsiElement? {
    val jetFile = KotlinPsiManager.INSTANCE.getParsedFile(file)
    val jetImportDirective = jetFile.getImportDirectives()
    return if (jetImportDirective.isNotEmpty()) jetImportDirective.last() else jetFile.getPackageDirective()
}