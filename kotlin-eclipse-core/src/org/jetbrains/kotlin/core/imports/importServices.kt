package org.jetbrains.kotlin.core.imports

import com.intellij.psi.PsiElement
import org.eclipse.jdt.core.Flags
import org.eclipse.jdt.core.IMethod
import org.eclipse.jdt.core.search.*
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.core.utils.isImported
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.diagnostics.Errors

val FIXABLE_DIAGNOSTICS = setOf(Errors.UNRESOLVED_REFERENCE, Errors.UNRESOLVED_REFERENCE_WRONG_RECEIVER)

fun findImportCandidates(
    references: List<PsiElement>,
    module: ModuleDescriptor,
    candidatesFilter: (ImportCandidate) -> Boolean
): UniqueAndAmbiguousImports {
    // Import candidates grouped by their ambiguity:
    // 0 - no candidates found, 1 - exactly one candidate, 2 - multiple candidates
    val groupedCandidates: Map<Int, List<List<ImportCandidate>>> =
        references.map { findImportCandidatesForReference(it, module, candidatesFilter) }
            .groupBy { it.size.coerceAtMost(2) }

    return UniqueAndAmbiguousImports(
        groupedCandidates[1].orEmpty().map { it.single() },
        groupedCandidates[2].orEmpty()
    )
}

fun findImportCandidatesForReference(
    reference: PsiElement,
    module: ModuleDescriptor,
    candidatesFilter: (ImportCandidate) -> Boolean
): List<ImportCandidate> =
    (findApplicableTypes(reference.text) + findApplicableCallables(reference, module))
        .filter(candidatesFilter)
        .distinctBy { it.fullyQualifiedName }


private fun findApplicableTypes(typeName: String): List<TypeCandidate> {
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

private fun findApplicableCallables(
    element: PsiElement,
    module: ModuleDescriptor
): List<FunctionCandidate> {
    return searchCallableByName(element)
        .let { module.accept(FunctionImportFinder(), it) }
        .map { FunctionCandidate(it) }
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

    return result
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

class DefaultImportPredicate(
    platform: TargetPlatform,
    languageVersionSettings: LanguageVersionSettings
) : (ImportCandidate) -> Boolean {
    private val defaultImports = platform.getDefaultImports(languageVersionSettings, true)

    override fun invoke(candidate: ImportCandidate): Boolean =
        candidate.fullyQualifiedName
            ?.let { ImportPath.fromString(it) }
            ?.isImported(defaultImports)
            ?.not()
            ?: false
}