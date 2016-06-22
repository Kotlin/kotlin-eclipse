package org.jetbrains.kotlin.core.resolve

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.config.LanguageFeatureSettings
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.createContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.descriptors.PackagePartProvider
import org.jetbrains.kotlin.frontend.di.configureModule
import org.jetbrains.kotlin.frontend.java.di.ContainerForTopDownAnalyzerForJvm
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.java.InternalFlexibleTypeTransformer
import org.jetbrains.kotlin.load.java.JavaClassFinderImpl
import org.jetbrains.kotlin.load.java.components.JavaPropertyInitializerEvaluatorImpl
import org.jetbrains.kotlin.load.java.components.JavaSourceElementFactoryImpl
import org.jetbrains.kotlin.load.java.components.LazyResolveBasedCache
import org.jetbrains.kotlin.load.java.components.PsiBasedExternalAnnotationResolver
import org.jetbrains.kotlin.load.java.components.SignaturePropagatorImpl
import org.jetbrains.kotlin.load.java.components.TraceBasedErrorReporter
import org.jetbrains.kotlin.load.java.lazy.SingleModuleClassResolver
import org.jetbrains.kotlin.load.java.sam.SamConversionResolverImpl
import org.jetbrains.kotlin.load.kotlin.DeserializationComponentsForJava
import org.jetbrains.kotlin.load.kotlin.JvmVirtualFileFinderFactory
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzerForTopLevel
import org.jetbrains.kotlin.resolve.jvm.JavaClassFinderPostConstruct
import org.jetbrains.kotlin.resolve.jvm.JavaDescriptorResolver
import org.jetbrains.kotlin.resolve.jvm.JavaLazyAnalyzerPostConstruct
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.resolve.lazy.FileScopeProviderImpl
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory

fun StorageComponentContainer.configureJavaTopDownAnalysisForScript(
        moduleContentScope: GlobalSearchScope,
        project: Project,
        lookupTracker: LookupTracker,
        languageFeatureSettings: LanguageFeatureSettings
) {
    useInstance(moduleContentScope)
    useInstance(lookupTracker)
    useImpl<ResolveSession>()

    useImpl<LazyTopDownAnalyzer>()
    useImpl<LazyTopDownAnalyzerForTopLevel>()
    useImpl<JavaDescriptorResolver>()
    useImpl<DeserializationComponentsForJava>()

    useInstance(JvmVirtualFileFinderFactory.SERVICE.getInstance(project).create(moduleContentScope))

    useImpl<FileScopeProviderImpl>()

    useImpl<JavaClassFinderImpl>()
    useImpl<SignaturePropagatorImpl>()
    useImpl<LazyResolveBasedCache>()
    useImpl<TraceBasedErrorReporter>()
    useImpl<PsiBasedExternalAnnotationResolver>()
    useImpl<JavaPropertyInitializerEvaluatorImpl>()
    useInstance(SamConversionResolverImpl)
    useImpl<JavaSourceElementFactoryImpl>()
    useImpl<JavaLazyAnalyzerPostConstruct>()
    useInstance(InternalFlexibleTypeTransformer)

    useInstance(languageFeatureSettings)
}

fun createContainerForTopDownAnalyzerForScript(
        moduleContext: ModuleContext,
        bindingTrace: BindingTrace,
        declarationProviderFactory: DeclarationProviderFactory,
        moduleContentScope: GlobalSearchScope,
        lookupTracker: LookupTracker,
        packagePartProvider: PackagePartProvider,
        languageFeatureSettings: LanguageFeatureSettings
): Pair<ContainerForTopDownAnalyzerForJvm, StorageComponentContainer> = createContainer("TopDownAnalyzerForJvm") {
    useInstance(packagePartProvider)

    configureModule(moduleContext, JvmPlatform, bindingTrace)
    configureJavaTopDownAnalysisForScript(moduleContentScope, moduleContext.project, lookupTracker, languageFeatureSettings)

    useInstance(declarationProviderFactory)

    CompilerEnvironment.configure(this)

    useImpl<SingleModuleClassResolver>()
}.let {
    it.javaAnalysisForScriptInit()

    Pair(ContainerForTopDownAnalyzerForJvm(it), it)
}

fun StorageComponentContainer.javaAnalysisForScriptInit() {
    get<JavaClassFinderImpl>().initialize()
    get<JavaClassFinderPostConstruct>().postCreate()
}

