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

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.eclipse.core.resources.IFile
import org.eclipse.jdt.core.Flags
import org.eclipse.jdt.core.IMethod
import org.eclipse.jdt.core.search.*
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility
import org.eclipse.jdt.ui.ISharedImages
import org.eclipse.jdt.ui.JavaUI
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.TextUtilities
import org.eclipse.swt.graphics.Image
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.eclipse.ui.utils.IndenterUtil
import org.jetbrains.kotlin.eclipse.ui.utils.getEndLfOffset
import org.jetbrains.kotlin.eclipse.ui.utils.getTextDocumentOffset
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.ui.editors.KotlinEditor
import org.jetbrains.kotlin.ui.editors.organizeImports.FunctionCandidate
import org.jetbrains.kotlin.ui.editors.organizeImports.FunctionImportFinder
import org.jetbrains.kotlin.ui.editors.organizeImports.ImportCandidate
import org.jetbrains.kotlin.ui.editors.organizeImports.TypeCandidate
import org.jetbrains.kotlin.utils.keysToMap

object KotlinAutoImportQuickFix : KotlinDiagnosticQuickFix {
    override fun getResolutions(diagnostic: Diagnostic): List<KotlinMarkerResolution> {
        val typeName = diagnostic.psiElement.text
        return findApplicableTypes(typeName).map { KotlinAutoImportResolution(it.match) }
    }

    override fun canFix(diagnostic: Diagnostic): Boolean {
        return diagnostic.factory == Errors.UNRESOLVED_REFERENCE
    }
}

fun findApplicableTypes(typeName: String): List<TypeCandidate> {
    val scope = SearchEngine.createWorkspaceScope()

    val foundTypes = arrayListOf<TypeNameMatch>()
    val collector = object : TypeNameMatchRequestor() {
        override fun acceptTypeNameMatch(match: TypeNameMatch) {
            if (Flags.isPublic(match.modifiers)) {
                foundTypes.add(match)
            }
        }
    }

    val searchEngine = SearchEngine()
    searchEngine.searchAllTypeNames(
        null,
        SearchPattern.R_EXACT_MATCH,
        typeName.toCharArray(),
        SearchPattern.R_EXACT_MATCH or SearchPattern.R_CASE_SENSITIVE,
        IJavaSearchConstants.TYPE,
        scope,
        collector,
        IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH,
        null
    )

    return foundTypes.map(::TypeCandidate)
}

fun findApplicableCallables(
    elements: List<PsiElement>,
    module: ModuleDescriptor
): Map<PsiElement, List<FunctionCandidate>> {
    return elements.keysToMap(::searchCallableByName)
        .mapValues { (_, v) -> module.accept(FunctionImportFinder(), v).map(::FunctionCandidate) }
}

private fun searchCallableByName(element: PsiElement): List<String> {
    val result = mutableListOf<String>()

    val conventionOperatorName = tryFindConventionOperatorName(element)

    if (conventionOperatorName != null) {
        queryForCallables(conventionOperatorName) {
            result += "${it.declaringType.fullyQualifiedName}.$conventionOperatorName"
        }
    } else {
        queryForCallables(element.text) {
            result += "${it.declaringType.fullyQualifiedName}.${it.elementName}"
        }

        if (element is KtNameReferenceExpression) {
            // We have to look for properties even if reference expression is first element in call expression,
            // because `something()` can mean `getSomething().invoke()`.
            queryForCallables(JvmAbi.getterName(element.text)) {
                result += "${it.declaringType.fullyQualifiedName}.${element.text}"
            }
        }
    }

    return result.also { println("${element.text} -> $it") }
}

private fun tryFindConventionOperatorName(element: PsiElement): String? {
    val isBinary = element.parent is KtBinaryExpression
    val isUnary = element.parent is KtPrefixExpression

    if (!isBinary && !isUnary) return null

    return (element as? KtOperationReferenceExpression)
        ?.operationSignTokenType
        ?.let { OperatorConventions.getNameForOperationSymbol(it, isUnary, isBinary) }
        ?.asString()
}

private fun queryForCallables(name: String, collector: (IMethod) -> Unit) {
    val pattern = SearchPattern.createPattern(
        name,
        IJavaSearchConstants.METHOD,
        IJavaSearchConstants.DECLARATIONS,
        SearchPattern.R_EXACT_MATCH
    )

    val requester = object : SearchRequestor() {
        override fun acceptSearchMatch(match: SearchMatch?) {
            (match?.element as? IMethod)?.also(collector)
        }
    }

    SearchEngine().search(
        pattern,
        arrayOf(SearchEngine.getDefaultSearchParticipant()),
        SearchEngine.createWorkspaceScope(),
        requester,
        null
    )
}

fun placeImports(chosenCandidates: List<ImportCandidate>, file: IFile, document: IDocument): Int {
    return placeStrImports(chosenCandidates.mapNotNull { it.fullyQualifiedName }, file, document)
}

fun replaceImports(newImports: List<String>, file: IFile, document: IDocument) {
    val ktFile = KotlinPsiManager.getParsedFile(file)
    val importDirectives = ktFile.importDirectives
    if (importDirectives.isEmpty()) {
        placeStrImports(newImports, file, document)
        return
    }

    val imports = buildImportsStr(newImports, document)

    val startOffset = importDirectives.first().getTextDocumentOffset(document)
    val lastImportDirectiveOffset = importDirectives.last().getEndLfOffset(document)
    val endOffset = if (newImports.isEmpty()) {
        val next = ktFile.importList!!.getNextSibling()
        if (next is PsiWhiteSpace) next.getEndLfOffset(document) else lastImportDirectiveOffset
    } else {
        lastImportDirectiveOffset
    }

    document.replace(startOffset, endOffset - startOffset, imports)
}

private fun placeStrImports(importsDirectives: List<String>, file: IFile, document: IDocument): Int {
    if (importsDirectives.isEmpty()) return -1

    val placeElement = findNodeToNewImport(file)
    if (placeElement == null) return -1

    val breakLineBefore = computeBreakLineBeforeImport(placeElement)
    val breakLineAfter = computeBreakLineAfterImport(placeElement)

    val lineDelimiter = TextUtilities.getDefaultLineDelimiter(document)

    val imports = buildImportsStr(importsDirectives, document)
    val newImports = "${IndenterUtil.createWhiteSpace(0, breakLineBefore, lineDelimiter)}$imports" +
            "${IndenterUtil.createWhiteSpace(0, breakLineAfter, lineDelimiter)}"

    document.replace(placeElement.getEndLfOffset(document), 0, newImports)

    return newImports.length
}

private fun buildImportsStr(importsDirectives: List<String>, document: IDocument): String {
    val lineDelimiter = TextUtilities.getDefaultLineDelimiter(document)
    return importsDirectives.map { "import ${it}" }.joinToString(lineDelimiter)
}

class KotlinAutoImportResolution(private val type: TypeNameMatch) : KotlinMarkerResolution {
    override fun apply(file: IFile) {
        val editor = EditorUtility.openInEditor(file, true) as KotlinEditor
        placeImports(listOf(TypeCandidate(type)), file, editor.document)
    }

    override fun getLabel(): String? = "Import '${type.simpleTypeName}' (${type.packageName})"

    override fun getImage(): Image? = JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_IMPDECL)
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

private fun countBreakLineAfterImportList(psiElement: PsiElement): Int {
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

private fun computeBreakLineBeforeImport(element: PsiElement): Int {
    if (element is KtPackageDirective) {
        return when {
            element.isRoot() -> 0
            else -> 2
        }
    }

    return 1
}

private fun findNodeToNewImport(file: IFile): PsiElement? {
    val jetFile = KotlinPsiManager.getParsedFile(file)
    val jetImportDirective = jetFile.importDirectives
    return if (jetImportDirective.isNotEmpty()) jetImportDirective.last() else jetFile.packageDirective
}