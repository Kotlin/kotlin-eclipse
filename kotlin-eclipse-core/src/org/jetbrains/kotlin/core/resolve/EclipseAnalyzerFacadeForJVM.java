/*******************************************************************************
 * Copyright 2000-2014 JetBrains s.r.o.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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

package org.jetbrains.kotlin.core.resolve;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import kotlin.Function1;

import org.eclipse.jdt.core.IJavaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.analyzer.ModuleContent;
import org.jetbrains.jet.analyzer.ModuleInfo;
import org.jetbrains.jet.analyzer.PlatformAnalysisParameters;
import org.jetbrains.jet.analyzer.ResolverForModule;
import org.jetbrains.jet.analyzer.ResolverForProject;
import org.jetbrains.jet.analyzer.ResolverForProjectImpl;
import org.jetbrains.jet.context.ContextPackage;
import org.jetbrains.jet.context.GlobalContext;
import org.jetbrains.jet.lang.descriptors.PackageFragmentProvider;
import org.jetbrains.jet.lang.descriptors.impl.CompositePackageFragmentProvider;
import org.jetbrains.jet.lang.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.TopDownAnalysisParameters;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.java.JvmPlatformParameters;
import org.jetbrains.jet.lang.resolve.java.JvmResolverForModule;
import org.jetbrains.jet.lang.resolve.java.lazy.ModuleClassResolver;
import org.jetbrains.jet.lang.resolve.java.lazy.ModuleClassResolverImpl;
import org.jetbrains.jet.lang.resolve.java.mapping.JavaToKotlinClassMap;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.lang.resolve.lazy.declarations.DeclarationProviderFactory;
import org.jetbrains.jet.lang.resolve.lazy.declarations.DeclarationProviderFactoryService;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.kotlin.core.injectors.EclipseInjectorForLazyResolveWithJava;
import org.jetbrains.kotlin.core.injectors.EclipseInjectorForTopDownAnalyzerForJvm;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;

public enum EclipseAnalyzerFacadeForJVM {

    INSTANCE;

    public static final List<ImportPath> DEFAULT_IMPORTS = ImmutableList.of(
            new ImportPath("java.lang.*"),
            new ImportPath("kotlin.*"),
            new ImportPath("kotlin.jvm.*"),
            new ImportPath("kotlin.io.*")
    );

    private EclipseAnalyzerFacadeForJVM() {
    }
    
    public <P extends PlatformAnalysisParameters, M extends ModuleInfo> ResolveSession createLazyResolveSession(
            @NotNull IJavaProject javaProject,
            @NotNull Project project,
            @NotNull GlobalContext globalContext,
            @NotNull M module,
            @NotNull ModuleContent moduleContent,
            @NotNull JvmPlatformParameters platformParameters) {
        ResolverForProjectImpl<M, JvmResolverForModule> resolverForProject = createResolverForProject(module);
        setupModuleDependencies(module, resolverForProject);
        
        ModuleDescriptorImpl descriptor = resolverForProject.descriptorForModule(module);
        JvmResolverForModule resolverForModule = 
                createResolverForModule(javaProject, project, globalContext, descriptor, moduleContent, platformParameters, resolverForProject);
        assert descriptor.getIsInitialized() : "ModuleDescriptorImpl#initialize() should be called in createResolverForModule";
        
        resolverForProject.getResolverByModuleDescriptor().put(descriptor, resolverForModule);
        
        return resolverForModule.getLazyResolveSession();
    }
    
    @SuppressWarnings("unchecked")
    private <A extends ResolverForModule, M extends ModuleInfo> void setupModuleDependencies(
            @NotNull M module, 
            @NotNull ResolverForProjectImpl<M, A> resolverForProject) {
        ModuleDescriptorImpl descriptorForModule = resolverForProject.descriptorForModule(module);
        List<ModuleDescriptorImpl> dependenciesDescriptors = Lists.newArrayList();
        for (ModuleInfo dependencyInfo : module.dependencies()) {
            dependenciesDescriptors.add(resolverForProject.descriptorForModule((M) dependencyInfo));
        }
        
        ModuleDescriptorImpl builtInsModule = KotlinBuiltIns.getInstance().getBuiltInsModule();
        module.dependencyOnBuiltins().adjustDependencies(builtInsModule, dependenciesDescriptors);
        for (ModuleDescriptorImpl dependency : dependenciesDescriptors) {
            descriptorForModule.addDependencyOnModule(dependency);
        }
        
        for (ModuleDescriptorImpl moduleDescriptor : resolverForProject.getDescriptorByModule().values()) {
           moduleDescriptor.seal();
        }
    }
    
    @NotNull
    private <M extends ModuleInfo, A extends ResolverForModule> ResolverForProjectImpl<M, A> createResolverForProject(
            @NotNull M module) {
        Map<M, ModuleDescriptorImpl> descriptorByModule = Maps.newHashMap();
        descriptorByModule.put(module, new ModuleDescriptorImpl(module.getName(), DEFAULT_IMPORTS, JavaToKotlinClassMap.getInstance()));
        
        return new ResolverForProjectImpl<>(descriptorByModule);
    }
    
    @NotNull
    private <M extends ModuleInfo> JvmResolverForModule createResolverForModule(
            @NotNull IJavaProject javaProject,
            @NotNull Project project,
            @NotNull GlobalContext globalContext,
            @NotNull ModuleDescriptorImpl moduleDescriptor,
            @NotNull ModuleContent moduleContent,
            @NotNull final JvmPlatformParameters platformParameters,
            @NotNull final ResolverForProject<M, JvmResolverForModule> resolverForProject) {
        DeclarationProviderFactory declarationProviderFactory = DeclarationProviderFactoryService.OBJECT$
                .createDeclarationProviderFactory(
                        project, 
                        globalContext.getStorageManager(), 
                        moduleContent.getSyntheticFiles(), 
                        moduleContent.getModuleContentScope());
        
        ModuleClassResolver moduleClassResolver = new ModuleClassResolverImpl(new Function1<JavaClass, JavaDescriptorResolver>() {
            @SuppressWarnings("unchecked")
            @Override
            public JavaDescriptorResolver invoke(JavaClass javaClass) {
                ModuleInfo moduleInfo = platformParameters.getModuleByJavaClass().invoke(javaClass);
                return resolverForProject.resolverForModule((M) moduleInfo).getJavaDescriptorResolver();
            }
        });
        
        EclipseInjectorForLazyResolveWithJava injector = new EclipseInjectorForLazyResolveWithJava(
                project,
                globalContext,
                moduleDescriptor,
                moduleContent.getModuleContentScope(),
                new BindingTraceContext(),
                declarationProviderFactory,
                moduleClassResolver,
                javaProject);
        
        ResolveSession resolveSession = injector.getResolveSession();
        JavaDescriptorResolver javaDescriptorResolver = injector.getJavaDescriptorResolver();
        List<PackageFragmentProvider> providersForModule = Arrays.asList(
                new PackageFragmentProvider[] {
                        resolveSession.getPackageFragmentProvider(), 
                        javaDescriptorResolver.getPackageFragmentProvider()});
        
        moduleDescriptor.initialize(new CompositePackageFragmentProvider(providersForModule));
        return new JvmResolverForModule(resolveSession, javaDescriptorResolver);
    }

    @NotNull
    public static AnalyzeExhaust analyzeFilesWithJavaIntegration(
            IJavaProject javaProject, 
            Project project,
            Collection<JetFile> files,
            BindingTrace trace,
            Predicate<PsiFile> filesToAnalyzeCompletely,
            ModuleDescriptorImpl module
    ) {
        GlobalContext globalContext = ContextPackage.GlobalContext();
        TopDownAnalysisParameters topDownAnalysisParameters = TopDownAnalysisParameters.create(
                globalContext.getStorageManager(),
                globalContext.getExceptionTracker(),
                filesToAnalyzeCompletely,
                false,
                false
        );

        EclipseInjectorForTopDownAnalyzerForJvm injector = new EclipseInjectorForTopDownAnalyzerForJvm(project, 
                topDownAnalysisParameters, trace, module, javaProject);
        try {
            List<PackageFragmentProvider> additionalProviders = Lists.newArrayList();
            additionalProviders.add(injector.getJavaDescriptorResolver().getPackageFragmentProvider());
            
            injector.getTopDownAnalyzer().analyzeFiles(topDownAnalysisParameters, files, additionalProviders);
            return AnalyzeExhaust.success(trace.getBindingContext(), module);
        }
        finally {
            injector.destroy();
        }
    }

    @NotNull
    public static ModuleDescriptorImpl createJavaModule(@NotNull String name) {
        return new ModuleDescriptorImpl(Name.special(name), DEFAULT_IMPORTS, JavaToKotlinClassMap.getInstance());
    }
}
