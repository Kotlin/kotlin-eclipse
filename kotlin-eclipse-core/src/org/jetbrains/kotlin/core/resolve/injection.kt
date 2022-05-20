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
package org.jetbrains.kotlin.core.resolve

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.eclipse.jdt.core.IJavaProject
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltIns
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInsPackageFragmentProvider
import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.registerSingleton
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.core.resolve.lang.java.EclipseJavaClassFinder
import org.jetbrains.kotlin.core.resolve.lang.java.resolver.EclipseJavaSourceElementFactory
import org.jetbrains.kotlin.core.resolve.lang.java.resolver.EclipseTraceBasedJavaResolverCache
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaPropertyInitializerEvaluator
import org.jetbrains.kotlin.frontend.di.configureModule
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.java.InternalFlexibleTypeTransformer
import org.jetbrains.kotlin.load.java.JavaClassesTracker
import org.jetbrains.kotlin.load.java.JavaModuleAnnotationsProvider
import org.jetbrains.kotlin.load.java.components.SignaturePropagatorImpl
import org.jetbrains.kotlin.load.java.components.TraceBasedErrorReporter
import org.jetbrains.kotlin.load.java.lazy.JavaResolverSettings
import org.jetbrains.kotlin.load.java.lazy.ModuleClassResolver
import org.jetbrains.kotlin.load.kotlin.DeserializationComponentsForJava
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.jvm.JavaDescriptorResolver
import org.jetbrains.kotlin.resolve.jvm.SyntheticJavaPartsProvider
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory

fun StorageComponentContainer.configureJavaTopDownAnalysis(
        moduleContentScope: GlobalSearchScope,
        project: Project,
        lookupTracker: LookupTracker,
        languageFeatureSettings: LanguageVersionSettings) {
    useInstance(moduleContentScope)
    useInstance(lookupTracker)

    useImpl<ResolveSession>()

    useImpl<LazyTopDownAnalyzer>()
    useImpl<JavaDescriptorResolver>()
    useImpl<DeserializationComponentsForJava>()

    useInstance(VirtualFileFinderFactory.SERVICE.getInstance(project).create(moduleContentScope))

    useImpl<EclipseJavaPropertyInitializerEvaluator>()
    useImpl<AnnotationResolverImpl>()
    useImpl<SignaturePropagatorImpl>()
    useImpl<TraceBasedErrorReporter>()
    useInstance(InternalFlexibleTypeTransformer)
}

fun createContainerForLazyResolveWithJava(
    moduleContext: ModuleContext,
    bindingTrace: BindingTrace,
    declarationProviderFactory: DeclarationProviderFactory,
    moduleContentScope: GlobalSearchScope,
    moduleClassResolver: ModuleClassResolver,
    targetEnvironment: TargetEnvironment,
    lookupTracker: LookupTracker,
    packagePartProvider: PackagePartProvider,
    jvmTarget: JvmTarget,
    languageVersionSettings: LanguageVersionSettings,
    javaProject: IJavaProject?,
    useBuiltInsProvider: Boolean,
    javaModuleAnnotationsProvider: JavaModuleAnnotationsProvider,
    analyzerService: PlatformDependentAnalyzerServices?
): StorageComponentContainer = createContainer("LazyResolveWithJava", JvmPlatformAnalyzerServices) {
    configureModule(
        moduleContext,
        JvmPlatforms.jvmPlatformByTargetVersion(jvmTarget),
        analyzerService ?: JvmPlatformAnalyzerServices,
        bindingTrace,
        languageVersionSettings
    )
    configureJavaTopDownAnalysis(moduleContentScope, moduleContext.project, lookupTracker, languageVersionSettings)

    useImpl<EclipseJavaClassFinder>()
    useImpl<EclipseTraceBasedJavaResolverCache>()
    useImpl<EclipseJavaSourceElementFactory>()

    useInstance(SyntheticJavaPartsProvider.EMPTY)
    useInstance(packagePartProvider)
    useInstance(moduleClassResolver)
    useInstance(javaModuleAnnotationsProvider)
    useInstance(declarationProviderFactory)
    javaProject?.let { useInstance(it) }

    //TODO???
    useInstance(languageVersionSettings.getFlag(JvmAnalysisFlags.javaTypeEnhancementState))

    if (useBuiltInsProvider) {
        //TODO???
        useInstance((moduleContext.module.builtIns as JvmBuiltIns).customizer)
        useImpl<JvmBuiltInsPackageFragmentProvider>()
    }

    useInstance(JavaClassesTracker.Default)

    targetEnvironment.configure(this)

    useInstance(
        JavaResolverSettings.create(
            //isReleaseCoroutines = languageVersionSettings.supportsFeature(LanguageFeature.ReleaseCoroutines),
            correctNullabilityForNotNullTypeParameter = false,
            typeEnhancementImprovementsInStrictMode = false,
            ignoreNullabilityForErasedValueParameters = false
        )
    )
}.apply {
    get<EclipseJavaClassFinder>().initialize(bindingTrace, get(),languageVersionSettings, jvmTarget)
}

fun createContainerForTopDownAnalyzerForJvm(
        moduleContext: ModuleContext,
        bindingTrace: BindingTrace,
        declarationProviderFactory: DeclarationProviderFactory,
        moduleContentScope: GlobalSearchScope,
        lookupTracker: LookupTracker,
        packagePartProvider: PackagePartProvider,
        jvmTarget: JvmTarget,
        languageVersionSettings: LanguageVersionSettings,
        moduleClassResolver: ModuleClassResolver,
        javaProject: IJavaProject?,
        javaModuleAnnotationsProvider: JavaModuleAnnotationsProvider,
        analyzerService: PlatformDependentAnalyzerServices?
): ComponentProvider = createContainerForLazyResolveWithJava(
        moduleContext, bindingTrace, declarationProviderFactory, moduleContentScope, moduleClassResolver,
        CompilerEnvironment, lookupTracker, packagePartProvider, jvmTarget, languageVersionSettings, javaProject,
        useBuiltInsProvider = true, javaModuleAnnotationsProvider, analyzerService
)

// Copy functions from Dsl.kt as they were shrinked by proguard
inline fun <reified T : Any> StorageComponentContainer.useImpl() {
    registerSingleton(T::class.java)
}

inline fun <reified T : Any> ComponentProvider.get(): T {
    return getService(T::class.java)
}