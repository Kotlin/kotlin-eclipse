package org.jetbrains.kotlin.ui.editors.completion

import com.intellij.psi.PsiElement
import kotlinx.coroutines.*
import org.eclipse.core.resources.IFile
import org.eclipse.jdt.core.Flags
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.core.search.*
import org.eclipse.jdt.internal.ui.search.JavaSearchScopeFactory
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.core.resolve.KotlinResolutionFacade
import org.jetbrains.kotlin.core.utils.ProjectUtils
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.eclipse.ui.utils.KotlinEclipseScope
import org.jetbrains.kotlin.eclipse.ui.utils.KotlinImageProvider
import org.jetbrains.kotlin.idea.FrontendInternals
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.util.*
import org.jetbrains.kotlin.incremental.KotlinLookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.load.kotlin.toSourceElement
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfoBefore
import org.jetbrains.kotlin.resolve.calls.smartcasts.SmartCastManager
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter.Companion.CALLABLES
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter.Companion.FUNCTIONS_MASK
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter.Companion.VARIABLES_MASK
import org.jetbrains.kotlin.resolve.scopes.receivers.ClassQualifier
import org.jetbrains.kotlin.resolve.scopes.utils.collectDescriptorsFiltered
import org.jetbrains.kotlin.resolve.scopes.utils.memberScopeAsImportingScope
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.DoubleColonLHS
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.ui.editors.codeassist.KotlinBasicCompletionProposal
import org.jetbrains.kotlin.ui.editors.codeassist.KotlinImportCallableCompletionProposal
import org.jetbrains.kotlin.ui.refactorings.extract.parentsWithSelf
import kotlin.time.ExperimentalTime

class KotlinReferenceVariantsHelper(
    val bindingContext: BindingContext,
    private val resolutionFacade: KotlinResolutionFacade,
    private val moduleDescriptor: ModuleDescriptor,
    val visibilityFilter: (DeclarationDescriptor) -> Boolean
) {
    fun getReferenceVariants(
        simpleNameExpression: KtSimpleNameExpression,
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
        javaProject: IJavaProject,
        ktFile: KtFile,
        file: IFile,
        identifierPart: String?
    ): Collection<KotlinBasicCompletionProposal> {
        val callTypeAndReceiver = CallTypeAndReceiver.detect(simpleNameExpression)
        var variants: Collection<KotlinBasicCompletionProposal> =
            getReferenceVariants(
                simpleNameExpression,
                callTypeAndReceiver,
                kindFilter,
                nameFilter,
                javaProject,
                ktFile,
                file,
                identifierPart
            ).filter {
                !resolutionFacade.frontendService<DeprecationResolver>().isHiddenInResolution(it.descriptor) &&
                        visibilityFilter(it.descriptor)
            }

        val tempFilter = ShadowedDeclarationsFilter.create(
            bindingContext,
            resolutionFacade,
            simpleNameExpression,
            callTypeAndReceiver
        )
        if (tempFilter != null) {
            variants = variants.mapNotNull {
                if (tempFilter.filter(listOf(it.descriptor)).isEmpty()) null else it
            }
        }

        return variants.filter { kindFilter.accepts(it.descriptor) }
    }

    private fun getVariantsForImportOrPackageDirective(
        receiverExpression: KtExpression?,
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        if (receiverExpression != null) {
            val qualifier = bindingContext[BindingContext.QUALIFIER, receiverExpression] ?: return emptyList()
            val staticDescriptors = qualifier.staticScope.collectStaticMembers(resolutionFacade, kindFilter, nameFilter)

            val objectDescriptor = (qualifier as? ClassQualifier)?.descriptor?.takeIf { it.kind == ClassKind.OBJECT }
                ?: return staticDescriptors

            return staticDescriptors + objectDescriptor.defaultType.memberScope.getDescriptorsFiltered(
                kindFilter,
                nameFilter
            )
        } else {
            val rootPackage = resolutionFacade.moduleDescriptor.getPackage(FqName.ROOT)
            return rootPackage.memberScope.getDescriptorsFiltered(kindFilter, nameFilter)
        }
    }

    private fun getVariantsForUserType(
        receiverExpression: KtExpression?,
        contextElement: PsiElement,
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        if (receiverExpression != null) {
            val qualifier = bindingContext[BindingContext.QUALIFIER, receiverExpression] ?: return emptyList()
            return qualifier.staticScope.collectStaticMembers(resolutionFacade, kindFilter, nameFilter)
        } else {
            val scope = contextElement.getResolutionScope(bindingContext, resolutionFacade)
            return scope.collectDescriptorsFiltered(kindFilter, nameFilter, changeNamesForAliased = true)
        }
    }

    private fun getVariantsForCallableReference(
        callTypeAndReceiver: CallTypeAndReceiver.CALLABLE_REFERENCE,
        contextElement: PsiElement,
        useReceiverType: KotlinType?,
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<KotlinBasicCompletionProposal> {
        val descriptors = LinkedHashSet<KotlinBasicCompletionProposal>()

        val resolutionScope = contextElement.getResolutionScope(bindingContext, resolutionFacade)

        val receiver = callTypeAndReceiver.receiver
        if (receiver != null) {
            val isStatic = bindingContext[BindingContext.DOUBLE_COLON_LHS, receiver] is DoubleColonLHS.Type

            val explicitReceiverTypes: Collection<KotlinType> = useReceiverType?.let {
                listOf(useReceiverType)
            } ?: callTypeAndReceiver.receiverTypes(
                bindingContext,
                contextElement,
                moduleDescriptor,
                resolutionFacade,
                stableSmartCastsOnly = false
            )!!

            val constructorFilter = { descriptor: ClassDescriptor -> if (isStatic) true else descriptor.isInner }
            descriptors.addNonExtensionMembers(explicitReceiverTypes, kindFilter, nameFilter, constructorFilter)

            descriptors.addScopeAndSyntheticExtensions(
                resolutionScope,
                explicitReceiverTypes,
                CallType.CALLABLE_REFERENCE,
                kindFilter,
                nameFilter
            )

            if (isStatic) {
                explicitReceiverTypes
                    .mapNotNull { (it.constructor.declarationDescriptor as? ClassDescriptor)?.staticScope }
                    .flatMapTo(descriptors) {
                        it.collectStaticMembers(resolutionFacade, kindFilter, nameFilter)
                            .map { KotlinBasicCompletionProposal.Descriptor(it) }
                    }
            }
        } else {
            descriptors.addNonExtensionCallablesAndConstructors(
                resolutionScope,
                kindFilter, nameFilter, constructorFilter = { !it.isInner },
                classesOnly = false
            )
        }
        return descriptors
    }

    private fun getReferenceVariants(
        simpleNameExpression: KtSimpleNameExpression,
        callTypeAndReceiver: CallTypeAndReceiver<*, *>,
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
        javaProject: IJavaProject,
        ktFile: KtFile,
        file: IFile,
        identifierPart: String?
    ): Collection<KotlinBasicCompletionProposal> {
        val callType = callTypeAndReceiver.callType

        @Suppress("NAME_SHADOWING")
        val kindFilter = kindFilter.intersect(callType.descriptorKindFilter)

        val receiverExpression: KtExpression?
        when (callTypeAndReceiver) {
            is CallTypeAndReceiver.IMPORT_DIRECTIVE -> {
                return getVariantsForImportOrPackageDirective(callTypeAndReceiver.receiver, kindFilter, nameFilter)
                    .map { KotlinBasicCompletionProposal.Descriptor(it) }
            }

            is CallTypeAndReceiver.PACKAGE_DIRECTIVE -> {
                return getVariantsForImportOrPackageDirective(callTypeAndReceiver.receiver, kindFilter, nameFilter)
                    .map { KotlinBasicCompletionProposal.Descriptor(it) }
            }

            is CallTypeAndReceiver.TYPE -> {
                return getVariantsForUserType(
                    callTypeAndReceiver.receiver,
                    simpleNameExpression,
                    kindFilter,
                    nameFilter
                ).map { KotlinBasicCompletionProposal.Descriptor(it) }
            }

            is CallTypeAndReceiver.ANNOTATION -> {
                return getVariantsForUserType(
                    callTypeAndReceiver.receiver,
                    simpleNameExpression,
                    kindFilter,
                    nameFilter
                ).map { KotlinBasicCompletionProposal.Descriptor(it) }
            }

            is CallTypeAndReceiver.CALLABLE_REFERENCE -> {
                return getVariantsForCallableReference(
                    callTypeAndReceiver,
                    simpleNameExpression,
                    null,
                    kindFilter,
                    nameFilter
                )
            }

            is CallTypeAndReceiver.DEFAULT -> receiverExpression = null
            is CallTypeAndReceiver.DOT -> receiverExpression = callTypeAndReceiver.receiver
            is CallTypeAndReceiver.SUPER_MEMBERS -> receiverExpression = callTypeAndReceiver.receiver
            is CallTypeAndReceiver.SAFE -> receiverExpression = callTypeAndReceiver.receiver
            is CallTypeAndReceiver.INFIX -> receiverExpression = callTypeAndReceiver.receiver
            is CallTypeAndReceiver.OPERATOR -> return emptyList()
            is CallTypeAndReceiver.UNKNOWN -> return emptyList()
            else -> throw RuntimeException()
        }

        val resolutionScope = simpleNameExpression.getResolutionScope(bindingContext, resolutionFacade)
        val dataFlowInfo = bindingContext.getDataFlowInfoBefore(simpleNameExpression)
        val containingDeclaration = resolutionScope.ownerDescriptor

        val smartCastManager = resolutionFacade.frontendService<SmartCastManager>()
        val languageVersionSettings = resolutionFacade.frontendService<LanguageVersionSettings>()

        val implicitReceiverTypes = resolutionScope.getImplicitReceiversWithInstance(
            languageVersionSettings.supportsFeature(LanguageFeature.DslMarkersSupport)
        ).flatMap {
            smartCastManager.getSmartCastVariantsWithLessSpecificExcluded(
                it.value,
                bindingContext,
                containingDeclaration,
                dataFlowInfo,
                languageVersionSettings,
                resolutionFacade.frontendService()
            )
        }.toSet()

        val descriptors = LinkedHashSet<KotlinBasicCompletionProposal>()

        val filterWithoutExtensions = kindFilter exclude DescriptorKindExclude.Extensions
        if (receiverExpression != null) {
            val qualifier = bindingContext[BindingContext.QUALIFIER, receiverExpression]
            if (qualifier != null) {
                descriptors.addAll(
                    qualifier.staticScope.collectStaticMembers(
                        resolutionFacade,
                        filterWithoutExtensions,
                        nameFilter
                    ).map { KotlinBasicCompletionProposal.Descriptor(it) }
                )
            }

            val explicitReceiverTypes = callTypeAndReceiver.receiverTypes(
                bindingContext,
                simpleNameExpression,
                moduleDescriptor,
                resolutionFacade,
                stableSmartCastsOnly = false
            )!!

            descriptors.processAll(
                implicitReceiverTypes,
                explicitReceiverTypes,
                resolutionScope,
                callType,
                kindFilter,
                nameFilter,
                javaProject,
                ktFile,
                file,
                identifierPart,
                false
            )
        } else {
            descriptors.processAll(
                implicitReceiverTypes,
                implicitReceiverTypes,
                resolutionScope,
                callType,
                kindFilter,
                nameFilter,
                javaProject,
                ktFile,
                file,
                identifierPart,
                true
            )

            descriptors.addAll(
                resolutionScope.collectDescriptorsFiltered(
                    filterWithoutExtensions,
                    nameFilter,
                    changeNamesForAliased = true
                ).map { KotlinBasicCompletionProposal.Descriptor(it) }
            )
        }

        if (callType == CallType.SUPER_MEMBERS) { // we need to unwrap fake overrides in case of "super." because ShadowedDeclarationsFilter does not work correctly
            return descriptors.filterIsInstance<KotlinBasicCompletionProposal.Descriptor>()
                .flatMapTo(LinkedHashSet<KotlinBasicCompletionProposal>()) {
                    if (it.descriptor is CallableMemberDescriptor && it.descriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
                        it.descriptor.overriddenDescriptors.map { KotlinBasicCompletionProposal.Descriptor(it) }
                    } else {
                        listOf(it)
                    }
                }
        }

        return descriptors.distinctBy { it.descriptor }
    }

    private fun MutableSet<KotlinBasicCompletionProposal>.processAll(
        implicitReceiverTypes: Collection<KotlinType>,
        receiverTypes: Collection<KotlinType>,
        resolutionScope: LexicalScope,
        callType: CallType<*>,
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
        javaProject: IJavaProject,
        ktFile: KtFile,
        file: IFile,
        identifierPart: String?,
        allowNoReceiver: Boolean
    ) {
        runBlocking {
            val tempJobs = mutableListOf<Job>()
            var tempJob = KotlinEclipseScope.launch {
                addNonExtensionMembers(receiverTypes, kindFilter, nameFilter, constructorFilter = { it.isInner })
            }
            tempJobs += tempJob
            tempJob = KotlinEclipseScope.launch {
                addMemberExtensions(implicitReceiverTypes, receiverTypes, callType, kindFilter, nameFilter)
            }
            tempJobs += tempJob
            tempJob = KotlinEclipseScope.launch {
                addNotImportedTopLevelCallables(
                    receiverTypes,
                    kindFilter,
                    nameFilter,
                    javaProject,
                    ktFile,
                    file,
                    identifierPart,
                    allowNoReceiver
                )
                println("Finished!")
            }
            tempJobs += tempJob
            tempJob = KotlinEclipseScope.launch {
                addScopeAndSyntheticExtensions(resolutionScope, receiverTypes, callType, kindFilter, nameFilter)
            }
            tempJobs += tempJob
            tempJobs.joinAll()
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun MutableSet<KotlinBasicCompletionProposal>.addNotImportedTopLevelCallables(
        receiverTypes: Collection<KotlinType>,
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
        javaProject: IJavaProject,
        ktFile: KtFile,
        file: IFile,
        identifierPart: String?,
        allowNoReceiver: Boolean
    ) {
        if (!identifierPart.isNullOrBlank()) {
            val searchEngine = SearchEngine()

            val dependencyProjects = arrayListOf<IJavaProject>().apply {
                addAll(ProjectUtils.getDependencyProjects(javaProject).map { JavaCore.create(it) })
                add(javaProject)
            }

            val javaProjectSearchScope =
                JavaSearchScopeFactory.getInstance().createJavaSearchScope(dependencyProjects.toTypedArray(), false)

            val tempClassNames = hashMapOf<String, String>()

            val collector = object : MethodNameMatchRequestor() {
                override fun acceptMethodNameMatch(match: MethodNameMatch) {
                    if (Flags.isPublic(match.modifiers)) {
                        tempClassNames[match.method.declaringType.typeQualifiedName] =
                            match.method.declaringType.packageFragment.elementName
                    }
                }
            }

            searchEngine.searchAllMethodNames(
                null,
                SearchPattern.R_EXACT_MATCH,
                null,
                SearchPattern.R_EXACT_MATCH,
                null,
                SearchPattern.R_EXACT_MATCH,
                identifierPart.toCharArray(),
                SearchPattern.R_PREFIX_MATCH,
                javaProjectSearchScope,
                collector,
                IJavaSearchConstants.FORCE_IMMEDIATE_SEARCH,
                null
            )

            searchEngine.searchAllMethodNames(
                null,
                SearchPattern.R_EXACT_MATCH,
                null,
                SearchPattern.R_EXACT_MATCH,
                null,
                SearchPattern.R_EXACT_MATCH,
                "get${identifierPart.capitalize()}".toCharArray(),
                SearchPattern.R_PREFIX_MATCH,
                javaProjectSearchScope,
                collector,
                IJavaSearchConstants.FORCE_IMMEDIATE_SEARCH,
                null
            )

            val tempPackages = tempClassNames.values.map {
                resolutionFacade.moduleDescriptor.getPackage(FqName(it))
            }

            val tempClasses = tempClassNames.mapNotNull { (className, packageName) ->
                val tempPackage = resolutionFacade.moduleDescriptor.getPackage(FqName(packageName))
                if (className.contains('$')) {
                    val tempNames = className.split('$')
                    val topLevelClass = tempNames.first()
                    var tempClassDescriptor = tempPackage.memberScope.getDescriptorsFiltered {
                        !it.isSpecial && it.identifier == topLevelClass
                    }.filterIsInstance<ClassDescriptor>().singleOrNull() ?: return@mapNotNull null

                    tempNames.drop(1).forEach { subName ->
                        tempClassDescriptor = tempClassDescriptor.unsubstitutedMemberScope.getDescriptorsFiltered {
                            !it.isSpecial && it.identifier == subName
                        }.filterIsInstance<ClassDescriptor>().singleOrNull() ?: return@mapNotNull null
                    }

                    tempClassDescriptor
                } else {
                    tempPackage.memberScope.getDescriptorsFiltered {
                        !it.isSpecial && it.identifier == className
                    }.filterIsInstance<ClassDescriptor>().singleOrNull()
                }
            }

            val importsSet = ktFile.importDirectives
                .mapNotNull { it.importedFqName?.asString() }
                .toSet()

            val originPackage = ktFile.packageFqName.asString()

            fun MemberScope.filterDescriptors() = getDescriptorsFiltered(
                kindFilter.intersect(CALLABLES),
                nameFilter
            ).asSequence()
                .filterIsInstance<CallableDescriptor>()
                .filter { callDesc ->
                    val tempFuzzy = callDesc.fuzzyExtensionReceiverType()
                    (allowNoReceiver && tempFuzzy == null) || (tempFuzzy != null && receiverTypes.any { receiverType ->
                        tempFuzzy.checkIsSuperTypeOf(receiverType) != null
                    })
                }
                .filter { callDesc ->
                    callDesc.importableFqName?.asString() !in importsSet &&
                            callDesc.importableFqName?.parent()?.asString() != originPackage
                }.toList()

            val tempDeferreds =
                tempPackages.map { desc -> KotlinEclipseScope.async { desc.memberScope.filterDescriptors() } } +
                        tempClasses.map { desc -> KotlinEclipseScope.async { desc.unsubstitutedMemberScope.filterDescriptors() } }

            val tempDescriptors = tempDeferreds.awaitAll().flatten()

            tempDescriptors
                .map {
                    KotlinBasicCompletionProposal.Proposal(
                        KotlinImportCallableCompletionProposal(
                            it,
                            KotlinImageProvider.getImage(it),
                            file,
                            identifierPart
                        ), it
                    )
                }.toCollection(this)
        }
    }

    private fun MutableSet<KotlinBasicCompletionProposal>.addMemberExtensions(
        dispatchReceiverTypes: Collection<KotlinType>,
        extensionReceiverTypes: Collection<KotlinType>,
        callType: CallType<*>,
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
    ) {
        val memberFilter = kindFilter exclude DescriptorKindExclude.NonExtensions
        for (dispatchReceiverType in dispatchReceiverTypes) {
            for (member in dispatchReceiverType.memberScope.getDescriptorsFiltered(memberFilter, nameFilter)) {
                addAll((member as CallableDescriptor).substituteExtensionIfCallable(extensionReceiverTypes, callType)
                    .map { KotlinBasicCompletionProposal.Descriptor(it) })
            }
        }
    }

    private fun MutableSet<KotlinBasicCompletionProposal>.addNonExtensionMembers(
        receiverTypes: Collection<KotlinType>,
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
        constructorFilter: (ClassDescriptor) -> Boolean
    ) {
        for (receiverType in receiverTypes) {
            addNonExtensionCallablesAndConstructors(
                receiverType.memberScope.memberScopeAsImportingScope(),
                kindFilter, nameFilter, constructorFilter,
                false
            )
            receiverType.constructor.supertypes.forEach {
                addNonExtensionCallablesAndConstructors(
                    it.memberScope.memberScopeAsImportingScope(),
                    kindFilter, nameFilter, constructorFilter,
                    true
                )
            }
        }
    }

    private fun MutableSet<KotlinBasicCompletionProposal>.addNonExtensionCallablesAndConstructors(
        scope: HierarchicalScope,
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
        constructorFilter: (ClassDescriptor) -> Boolean,
        classesOnly: Boolean
    ) {
        var filterToUse = DescriptorKindFilter(kindFilter.kindMask and DescriptorKindFilter.CALLABLES.kindMask).exclude(
            DescriptorKindExclude.Extensions
        )

        // should process classes if we need constructors
        if (filterToUse.acceptsKinds(FUNCTIONS_MASK)) {
            filterToUse = filterToUse.withKinds(DescriptorKindFilter.NON_SINGLETON_CLASSIFIERS_MASK)
        }

        for (descriptor in scope.collectDescriptorsFiltered(filterToUse, nameFilter, changeNamesForAliased = true)) {
            if (descriptor is ClassDescriptor) {
                if (descriptor.modality == Modality.ABSTRACT || descriptor.modality == Modality.SEALED) continue
                if (!constructorFilter(descriptor)) continue
                descriptor.constructors.map { KotlinBasicCompletionProposal.Descriptor(it) }
                    .filterTo(this) { kindFilter.accepts(it.descriptor) }
            } else if (!classesOnly && kindFilter.accepts(descriptor)) {
                this.add(KotlinBasicCompletionProposal.Descriptor(descriptor))
            }
        }
    }

    private fun MutableSet<KotlinBasicCompletionProposal>.addScopeAndSyntheticExtensions(
        scope: LexicalScope,
        receiverTypes: Collection<KotlinType>,
        callType: CallType<*>,
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ) {
        if (kindFilter.excludes.contains(DescriptorKindExclude.Extensions)) return
        if (receiverTypes.isEmpty()) return

        fun process(extensionOrSyntheticMember: CallableDescriptor) {
            if (kindFilter.accepts(extensionOrSyntheticMember) && nameFilter(extensionOrSyntheticMember.name)) {
                if (extensionOrSyntheticMember.isExtension) {
                    addAll(
                        extensionOrSyntheticMember.substituteExtensionIfCallable(receiverTypes, callType)
                            .map { KotlinBasicCompletionProposal.Descriptor(it) })
                } else {
                    add(KotlinBasicCompletionProposal.Descriptor(extensionOrSyntheticMember))
                }
            }
        }

        for (descriptor in scope.collectDescriptorsFiltered(
            kindFilter exclude DescriptorKindExclude.NonExtensions,
            nameFilter,
            changeNamesForAliased = true
        )) {
            process(descriptor as CallableDescriptor)
        }

        val syntheticScopes = resolutionFacade.getFrontendService(SyntheticScopes::class.java)
        if (kindFilter.acceptsKinds(VARIABLES_MASK)) {
            val lookupLocation =
                (scope.ownerDescriptor.toSourceElement.getPsi() as? KtElement)?.let { KotlinLookupLocation(it) }
                    ?: NoLookupLocation.FROM_IDE

            for (extension in syntheticScopes.collectSyntheticExtensionProperties(receiverTypes, lookupLocation)) {
                process(extension)
            }
        }

        if (kindFilter.acceptsKinds(FUNCTIONS_MASK)) {
            for (syntheticMember in syntheticScopes.collectSyntheticMemberFunctions(receiverTypes)) {
                process(syntheticMember)
            }
        }
    }

    private fun <TDescriptor : DeclarationDescriptor> filterOutJavaGettersAndSetters(variants: Collection<TDescriptor>): Collection<TDescriptor> {
        val accessorMethodsToRemove = HashSet<FunctionDescriptor>()
        val filteredVariants = variants.filter { it !is SyntheticJavaPropertyDescriptor }

        for (variant in filteredVariants) {
            if (variant is SyntheticJavaPropertyDescriptor) {
                accessorMethodsToRemove.add(variant.getMethod.original)

                val setter = variant.setMethod
                if (setter != null && setter.returnType?.isUnit() == true) { // we do not filter out non-Unit setters
                    accessorMethodsToRemove.add(setter.original)
                }
            }
        }

        return filteredVariants.filter { it !is FunctionDescriptor || it.original !in accessorMethodsToRemove }
    }

    private fun excludeNonInitializedVariable(
        variants: Collection<DeclarationDescriptor>,
        contextElement: PsiElement
    ): Collection<DeclarationDescriptor> {
        for (element in contextElement.parentsWithSelf) {
            val parent = element.parent
            if (parent is KtVariableDeclaration && element == parent.initializer) {
                return variants.filter { it.findPsi() != parent }
            }
            if (element is KtDeclaration) break // we can use variable inside lambda or anonymous object located in its initializer
        }
        return variants
    }
}

private fun MemberScope.collectStaticMembers(
    resolutionFacade: ResolutionFacade,
    kindFilter: DescriptorKindFilter,
    nameFilter: (Name) -> Boolean
): Collection<DeclarationDescriptor> {
    return getDescriptorsFiltered(kindFilter, nameFilter) + collectSyntheticStaticMembersAndConstructors(
        resolutionFacade,
        kindFilter,
        nameFilter
    )
}

@OptIn(FrontendInternals::class)
fun ResolutionScope.collectSyntheticStaticMembersAndConstructors(
    resolutionFacade: ResolutionFacade,
    kindFilter: DescriptorKindFilter,
    nameFilter: (Name) -> Boolean
): List<FunctionDescriptor> {
    val syntheticScopes = resolutionFacade.getFrontendService(SyntheticScopes::class.java)
    val functionDescriptors = this.getContributedDescriptors(DescriptorKindFilter.FUNCTIONS)
    val classifierDescriptors = this.getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS)
    return (syntheticScopes.collectSyntheticStaticFunctions(functionDescriptors) + syntheticScopes.collectSyntheticConstructors(
        classifierDescriptors
    ))
        .filter { kindFilter.accepts(it) && nameFilter(it.name) }
}

@OptIn(FrontendInternals::class)
private inline fun <reified T : Any> ResolutionFacade.frontendService(): T = this.getFrontendService(T::class.java)
