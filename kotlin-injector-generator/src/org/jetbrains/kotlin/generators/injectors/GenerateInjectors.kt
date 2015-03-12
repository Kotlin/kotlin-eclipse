package org.jetbrains.kotlin.generators.injectors

import org.jetbrains.kotlin.generators.di.DependencyInjectorGenerator
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzerForTopLevel
import org.jetbrains.kotlin.resolve.jvm.JavaDescriptorResolver
import org.jetbrains.kotlin.load.kotlin.DeserializationComponentsForJava
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder
import org.jetbrains.kotlin.generators.di.GivenExpression
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.load.java.components.TraceBasedExternalSignatureResolver
import org.jetbrains.kotlin.load.java.components.TraceBasedErrorReporter
import org.jetbrains.kotlin.load.java.sam.SamConversionResolverImpl
import org.jetbrains.kotlin.resolve.MutablePackageFragmentProvider
import org.jetbrains.kotlin.load.java.lazy.SingleModuleClassResolver
import org.jetbrains.kotlin.resolve.jvm.JavaLazyAnalyzerPostConstruct
import org.jetbrains.kotlin.load.java.JavaFlexibleTypeCapabilitiesProvider
import org.jetbrains.kotlin.load.kotlin.KotlinJvmCheckerProvider
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.context.GlobalContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.ScopeProvider
import org.eclipse.jdt.core.IJavaProject
import org.jetbrains.kotlin.core.resolve.lang.java.EclipseJavaClassFinder
import org.jetbrains.kotlin.core.resolve.lang.java.resolver.EclipseTraceBasedJavaResolverCache
import org.jetbrains.kotlin.core.resolve.lang.java.resolver.EclipseMethodSignatureChecker
import org.jetbrains.kotlin.core.resolve.lang.java.resolver.EclipseExternalAnnotationResolver
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaPropertyInitializerEvaluator
import org.jetbrains.kotlin.core.resolve.lang.java.resolver.EclipseJavaSourceElementFactory


// Copied from org.jetbrains.kotlin.generators.injectors.GenerateInjectors
// NOTE: After making changes, you need to re-generate the injectors.
//       To do that, you can run main in this file.
public fun main(args: Array<String>) {
    for (generator in createInjectorGenerators()) {
        try {
            generator.generate()
        }
        catch (e: Throwable) {
            System.err.println(generator.getOutputFile())
            throw e
        }
    }
}

public fun createInjectorGenerators(): List<DependencyInjectorGenerator> =
        listOf(
                generatorForTopDownAnalyzerForJvm()
        )


private fun generatorForTopDownAnalyzerForJvm() =
        generator("../kotlin-eclipse-core/src", "org.jetbrains.kotlin.core.injectors", "EclipseInjectorForTopDownAnalyzerForJvm") {
            commonForJavaTopDownAnalyzer()
        }


private fun DependencyInjectorGenerator.commonForResolveSessionBased() {
    parameter<Project>()
	parameter<IJavaProject>()
    parameter<GlobalContext>(useAsContext = true)
    parameter<BindingTrace>()
    publicParameter<ModuleDescriptorImpl>(name = "module", useAsContext = true)
    parameter<DeclarationProviderFactory>()

    publicField<ResolveSession>()
    field<ScopeProvider>()
}

private fun DependencyInjectorGenerator.commonForJavaTopDownAnalyzer() {
    commonForResolveSessionBased()

    parameter<GlobalSearchScope>(name = "moduleContentScope")

    publicField<LazyTopDownAnalyzer>()
    publicField<LazyTopDownAnalyzerForTopLevel>()
    publicField<JavaDescriptorResolver>()
    publicField<DeserializationComponentsForJava>()

    field<VirtualFileFinder>(
          init = GivenExpression(javaClass<VirtualFileFinderFactory>().getName()
                                 + ".SERVICE.getInstance(project).create(moduleContentScope)")
    )

    field<EclipseJavaClassFinder>()
    field<TraceBasedExternalSignatureResolver>()
    field<EclipseTraceBasedJavaResolverCache>()
    field<TraceBasedErrorReporter>()
    field<EclipseMethodSignatureChecker>()
    field<EclipseExternalAnnotationResolver>()
    field<EclipseJavaPropertyInitializerEvaluator>()
    field<SamConversionResolverImpl>()
    field<EclipseJavaSourceElementFactory>()
    field<MutablePackageFragmentProvider>()
    field<SingleModuleClassResolver>()
    field<JavaLazyAnalyzerPostConstruct>()
    field<JavaFlexibleTypeCapabilitiesProvider>()

    field<KotlinJvmCheckerProvider>()

    field<VirtualFileFinder>(init = GivenExpression(javaClass<VirtualFileFinder>().getName() + ".SERVICE.getInstance(project)"))
}


private fun generator(
        targetSourceRoot: String,
        injectorPackageName: String,
        injectorClassName: String,
        body: DependencyInjectorGenerator.() -> Unit
) = generator(targetSourceRoot, injectorPackageName, injectorClassName, "org.jetbrains.kotlin.generators.injectors.InjectorsPackage", body)
