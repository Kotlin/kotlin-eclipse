package org.jetbrains.kotlin.core.resolve

import org.jetbrains.kotlin.frontend.java.di.ContainerForTopDownAnalyzerForJvm
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.load.java.lazy.SingleModuleClassResolver
import org.jetbrains.kotlin.container.*
import org.jetbrains.kotlin.frontend.di.*
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzerForTopLevel
import org.jetbrains.kotlin.resolve.jvm.JavaDescriptorResolver
import org.jetbrains.kotlin.load.kotlin.DeserializationComponentsForJava
import org.jetbrains.kotlin.load.kotlin.JvmVirtualFileFinderFactory
import org.jetbrains.kotlin.load.java.JavaClassFinderImpl
import org.jetbrains.kotlin.load.java.components.LazyResolveBasedCache
import org.jetbrains.kotlin.load.java.components.TraceBasedErrorReporter
import org.jetbrains.kotlin.load.java.components.PsiBasedExternalAnnotationResolver
import org.jetbrains.kotlin.load.java.structure.impl.JavaPropertyInitializerEvaluatorImpl
import org.jetbrains.kotlin.load.java.sam.SamConversionResolverImpl
import org.jetbrains.kotlin.load.java.components.JavaSourceElementFactoryImpl
import org.jetbrains.kotlin.resolve.jvm.JavaLazyAnalyzerPostConstruct
import org.jetbrains.kotlin.load.java.JavaFlexibleTypeCapabilitiesProvider
import org.jetbrains.kotlin.resolve.jvm.JavaClassFinderPostConstruct
import org.jetbrains.kotlin.core.resolve.lang.java.EclipseJavaClassFinder
import org.jetbrains.kotlin.core.resolve.lang.java.resolver.EclipseTraceBasedJavaResolverCache
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaPropertyInitializerEvaluator
import org.jetbrains.kotlin.core.resolve.lang.java.resolver.EclipseExternalAnnotationResolver
import org.jetbrains.kotlin.core.resolve.lang.java.resolver.EclipseJavaSourceElementFactory
import org.eclipse.jdt.core.IJavaProject
import org.jetbrains.kotlin.resolve.lazy.FileScopeProviderImpl
import org.jetbrains.kotlin.resolve.BodyResolveCache
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.descriptors.PackagePartProvider
import org.jetbrains.kotlin.load.java.components.SignaturePropagatorImpl

fun StorageComponentContainer.configureJavaTopDownAnalysis(moduleContentScope: GlobalSearchScope, 
        project: Project, lookupTracker: LookupTracker) {
    useInstance(moduleContentScope)
    useInstance(lookupTracker)

    useImpl<ResolveSession>()

    useImpl<LazyTopDownAnalyzer>()
    useImpl<LazyTopDownAnalyzerForTopLevel>()
    useImpl<JavaDescriptorResolver>()
    useImpl<DeserializationComponentsForJava>()

    useInstance(JvmVirtualFileFinderFactory.SERVICE.getInstance(project).create(moduleContentScope))

    useImpl<EclipseJavaClassFinder>()
    useImpl<SignaturePropagatorImpl>()
    useImpl<EclipseTraceBasedJavaResolverCache>()
    useImpl<TraceBasedErrorReporter>()
    useImpl<EclipseExternalAnnotationResolver>()
    useImpl<EclipseJavaPropertyInitializerEvaluator>()
    useInstance(SamConversionResolverImpl)
    useImpl<EclipseJavaSourceElementFactory>()
    useImpl<JavaLazyAnalyzerPostConstruct>()
    useImpl<JavaFlexibleTypeCapabilitiesProvider>()
}

public fun createContainerForTopDownAnalyzerForJvm(
        moduleContext: ModuleContext, bindingTrace: BindingTrace,
        declarationProviderFactory: DeclarationProviderFactory,
        moduleContentScope: GlobalSearchScope,
        javaProject: IJavaProject,
        lookupTracker: LookupTracker,
        packagePartProvider: PackagePartProvider
): Pair<ContainerForTopDownAnalyzerForJvm, StorageComponentContainer> = createContainer("TopDownAnalyzerForJvm") {
    useInstance(packagePartProvider)
    
    configureModule(moduleContext, JvmPlatform, bindingTrace)
    configureJavaTopDownAnalysis(moduleContentScope, moduleContext.project, lookupTracker)
    
    useInstance(javaProject)
    useInstance(declarationProviderFactory)
    
    CompilerEnvironment.configure(this)

    useImpl<SingleModuleClassResolver>()
    useImpl<FileScopeProviderImpl>()
}.let {
    it.javaAnalysisInit()

    Pair(ContainerForTopDownAnalyzerForJvm(it), it)
}

private fun StorageComponentContainer.javaAnalysisInit() {
    get<JavaClassFinderPostConstruct>().postCreate()
}

// Copy functions from Dsl.kt as they were shrinked by proguard
private inline fun <reified T : Any> StorageComponentContainer.useImpl() {
    registerSingleton(T::class.java)
}

private inline fun <reified T : Any> ComponentProvider.get(): T {
    return getService(T::class.java)
}