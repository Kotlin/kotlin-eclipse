package org.jetbrains.kotlin.core.imports

import com.intellij.psi.PsiElement
import org.eclipse.jdt.core.Flags
import org.eclipse.jdt.core.IMethod
import org.eclipse.jdt.core.search.*
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.core.resolve.KotlinResolutionFacade
import org.jetbrains.kotlin.core.utils.isImported
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.imports.canBeReferencedViaImport
import org.jetbrains.kotlin.idea.util.CallTypeAndReceiver
import org.jetbrains.kotlin.idea.util.ReceiverType
import org.jetbrains.kotlin.idea.util.receiverTypesWithIndex
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.diagnostics.Errors

val FIXABLE_DIAGNOSTICS = setOf(Errors.UNRESOLVED_REFERENCE, Errors.UNRESOLVED_REFERENCE_WRONG_RECEIVER)

fun findImportCandidates(
    references: List<PsiElement>,
    bindingContext: BindingContext,
    resolutionFacade: KotlinResolutionFacade,
    candidatesFilter: (ImportCandidate) -> Boolean
): UniqueAndAmbiguousImports {
    // Import candidates grouped by their ambiguity:
    // 0 - no candidates found, 1 - exactly one candidate, 2 - multiple candidates
    val groupedCandidates: Map<Int, List<List<ImportCandidate>>> =
        references.map { findImportCandidatesForReference(it, bindingContext, resolutionFacade, candidatesFilter) }
            .groupBy { it.size.coerceAtMost(2) }

    return UniqueAndAmbiguousImports(
        groupedCandidates[1].orEmpty().map { it.single() },
        groupedCandidates[2].orEmpty()
    )
}

fun findImportCandidatesForReference(
    reference: PsiElement,
	bindingContext: BindingContext,
    resolutionFacade: KotlinResolutionFacade,
    candidatesFilter: (ImportCandidate) -> Boolean
): List<ImportCandidate> =
    (findApplicableTypes(reference.text) + findApplicableCallables(reference, bindingContext, resolutionFacade))
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
	bindingContext : BindingContext,
    resolutionFacade: KotlinResolutionFacade
): List<FunctionCandidate> {
    val module = resolutionFacade.moduleDescriptor

    val callTypeAndReceiver = (element as? KtSimpleNameExpression)
        ?.let { CallTypeAndReceiver.detect(it) }
        ?: return emptyList()

    val descriptorKindFilter = callTypeAndReceiver.callType.descriptorKindFilter

    val receiverTypes: Collection<ReceiverType> = callTypeAndReceiver
        .receiverTypesWithIndex(bindingContext, element, module, resolutionFacade, false)
        .orEmpty()

    val visitor = FunctionImportFinder { descriptor ->
        descriptor.canBeReferencedViaImport() && descriptorKindFilter.accepts(descriptor) && (
                descriptor.extensionReceiverParameter == null
                        || receiverTypes.any { descriptor.isReceiverTypeMatching(it.type) })
    }

    return searchCallableByName(element)
        .let { module.accept(visitor, it) }
        .map { FunctionCandidate(it) }
}

private fun CallableDescriptor.isReceiverTypeMatching(type: KotlinType): Boolean =
    extensionReceiverParameter?.let { type.isSubtypeOf(it.type) } ?: true

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