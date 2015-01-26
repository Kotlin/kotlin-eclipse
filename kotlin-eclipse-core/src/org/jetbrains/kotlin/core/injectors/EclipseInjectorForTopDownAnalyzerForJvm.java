/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
 */

package org.jetbrains.kotlin.core.injectors;

import com.intellij.openapi.project.Project;
import org.eclipse.jdt.core.IJavaProject;
import org.jetbrains.kotlin.context.GlobalContext;
import org.jetbrains.kotlin.storage.StorageManager;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.platform.PlatformToKotlinClassMap;
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory;
import org.jetbrains.kotlin.resolve.lazy.ResolveSession;
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer;
import org.jetbrains.kotlin.resolve.jvm.JavaDescriptorResolver;
import org.jetbrains.kotlin.load.kotlin.DeserializationComponentsForJava;
import org.jetbrains.kotlin.resolve.AdditionalCheckerProvider;
import org.jetbrains.kotlin.core.resolve.lang.java.EclipseJavaClassFinder;
import org.jetbrains.kotlin.load.java.components.TraceBasedExternalSignatureResolver;
import org.jetbrains.kotlin.core.resolve.lang.java.resolver.EclipseTraceBasedJavaResolverCache;
import org.jetbrains.kotlin.load.java.components.TraceBasedErrorReporter;
import org.jetbrains.kotlin.core.resolve.lang.java.resolver.EclipseMethodSignatureChecker;
import org.jetbrains.kotlin.core.resolve.lang.java.resolver.EclipseExternalAnnotationResolver;
import org.jetbrains.kotlin.load.java.sam.SamConversionResolverImpl;
import org.jetbrains.kotlin.resolve.MutablePackageFragmentProvider;
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaPropertyInitializerEvaluator;
import org.jetbrains.kotlin.core.resolve.lang.java.resolver.EclipseJavaSourceElementFactory;
import org.jetbrains.kotlin.resolve.jvm.JavaLazyAnalyzerPostConstruct;
import org.jetbrains.kotlin.load.java.lazy.SingleModuleClassResolver;
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder;
import org.jetbrains.kotlin.resolve.AnnotationResolver;
import org.jetbrains.kotlin.resolve.calls.CallResolver;
import org.jetbrains.kotlin.resolve.calls.ArgumentTypeResolver;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingComponents;
import org.jetbrains.kotlin.types.expressions.ControlStructureTypingUtils;
import org.jetbrains.kotlin.types.DynamicTypesSettings;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils;
import org.jetbrains.kotlin.types.expressions.ForLoopConventionsChecker;
import org.jetbrains.kotlin.types.expressions.LocalClassifierAnalyzer;
import org.jetbrains.kotlin.types.reflect.ReflectionTypes;
import org.jetbrains.kotlin.resolve.calls.CallExpressionResolver;
import org.jetbrains.kotlin.resolve.DescriptorResolver;
import org.jetbrains.kotlin.resolve.DelegatedPropertyResolver;
import org.jetbrains.kotlin.resolve.TypeResolver;
import org.jetbrains.kotlin.resolve.QualifiedExpressionResolver;
import org.jetbrains.kotlin.resolve.TypeResolver.FlexibleTypeCapabilitiesProvider;
import org.jetbrains.kotlin.context.LazinessToken;
import org.jetbrains.kotlin.resolve.PartialBodyResolveProvider;
import org.jetbrains.kotlin.resolve.calls.CallCompleter;
import org.jetbrains.kotlin.resolve.calls.CandidateResolver;
import org.jetbrains.kotlin.resolve.calls.tasks.TaskPrioritizer;
import org.jetbrains.kotlin.psi.JetImportsFactory;
import org.jetbrains.kotlin.resolve.lazy.ScopeProvider;
import org.jetbrains.kotlin.resolve.lazy.ScopeProvider.AdditionalFileScopeProvider;
import org.jetbrains.kotlin.resolve.ScriptBodyResolver;
import org.jetbrains.kotlin.resolve.BodyResolver;
import org.jetbrains.kotlin.resolve.ControlFlowAnalyzer;
import org.jetbrains.kotlin.resolve.DeclarationsChecker;
import org.jetbrains.kotlin.resolve.ModifiersChecker;
import org.jetbrains.kotlin.resolve.FunctionAnalyzerExtension;
import org.jetbrains.kotlin.resolve.DeclarationResolver;
import org.jetbrains.kotlin.resolve.ImportsResolver;
import org.jetbrains.kotlin.resolve.OverloadResolver;
import org.jetbrains.kotlin.resolve.OverrideResolver;
import org.jetbrains.kotlin.resolve.TopDownAnalyzer;
import org.jetbrains.kotlin.resolve.TypeHierarchyResolver;
import org.jetbrains.kotlin.resolve.ScriptHeaderResolver;
import org.jetbrains.kotlin.resolve.varianceChecker.VarianceChecker;
import org.jetbrains.kotlin.load.java.lazy.LazyJavaPackageFragmentProvider;
import org.jetbrains.kotlin.load.java.lazy.GlobalJavaResolverContext;
import org.jetbrains.kotlin.load.kotlin.DeserializedDescriptorResolver;
import org.jetbrains.kotlin.load.kotlin.JavaClassDataFinder;
import org.jetbrains.kotlin.load.kotlin.BinaryClassAnnotationAndConstantLoaderImpl;
import org.jetbrains.annotations.NotNull;
import javax.annotation.PreDestroy;

/* This file is generated by org.jetbrains.kotlin.core.injectors.InjectorsGenerator. DO NOT EDIT! */
@SuppressWarnings("all")
public class EclipseInjectorForTopDownAnalyzerForJvm {

    private final Project project;
    private final IJavaProject IJavaProject;
    private final GlobalContext globalContext;
    private final StorageManager storageManager;
    private final BindingTrace bindingTrace;
    private final ModuleDescriptorImpl moduleDescriptor;
    private final KotlinBuiltIns kotlinBuiltIns;
    private final PlatformToKotlinClassMap platformToKotlinClassMap;
    private final DeclarationProviderFactory declarationProviderFactory;
    private final ResolveSession resolveSession;
    private final LazyTopDownAnalyzer lazyTopDownAnalyzer;
    private final JavaDescriptorResolver javaDescriptorResolver;
    private final DeserializationComponentsForJava deserializationComponentsForJava;
    private final AdditionalCheckerProvider additionalCheckerProvider;
    private final EclipseJavaClassFinder eclipseJavaClassFinder;
    private final TraceBasedExternalSignatureResolver traceBasedExternalSignatureResolver;
    private final EclipseTraceBasedJavaResolverCache eclipseTraceBasedJavaResolverCache;
    private final TraceBasedErrorReporter traceBasedErrorReporter;
    private final EclipseMethodSignatureChecker eclipseMethodSignatureChecker;
    private final EclipseExternalAnnotationResolver eclipseExternalAnnotationResolver;
    private final SamConversionResolverImpl samConversionResolver;
    private final MutablePackageFragmentProvider mutablePackageFragmentProvider;
    private final EclipseJavaPropertyInitializerEvaluator eclipseJavaPropertyInitializerEvaluator;
    private final EclipseJavaSourceElementFactory eclipseJavaSourceElementFactory;
    private final JavaLazyAnalyzerPostConstruct javaLazyAnalyzerPostConstruct;
    private final SingleModuleClassResolver singleModuleClassResolver;
    private final VirtualFileFinder virtualFileFinder;
    private final AnnotationResolver annotationResolver;
    private final CallResolver callResolver;
    private final ArgumentTypeResolver argumentTypeResolver;
    private final ExpressionTypingServices expressionTypingServices;
    private final ExpressionTypingComponents expressionTypingComponents;
    private final ControlStructureTypingUtils controlStructureTypingUtils;
    private final DynamicTypesSettings dynamicTypesSettings;
    private final ExpressionTypingUtils expressionTypingUtils;
    private final ForLoopConventionsChecker forLoopConventionsChecker;
    private final LocalClassifierAnalyzer localClassifierAnalyzer;
    private final ReflectionTypes reflectionTypes;
    private final CallExpressionResolver callExpressionResolver;
    private final DescriptorResolver descriptorResolver;
    private final DelegatedPropertyResolver delegatedPropertyResolver;
    private final TypeResolver typeResolver;
    private final QualifiedExpressionResolver qualifiedExpressionResolver;
    private final FlexibleTypeCapabilitiesProvider flexibleTypeCapabilitiesProvider;
    private final LazinessToken lazinessToken;
    private final PartialBodyResolveProvider partialBodyResolveProvider;
    private final CallCompleter callCompleter;
    private final CandidateResolver candidateResolver;
    private final TaskPrioritizer taskPrioritizer;
    private final JetImportsFactory jetImportsFactory;
    private final ScopeProvider scopeProvider;
    private final AdditionalFileScopeProvider additionalFileScopeProvider;
    private final ScriptBodyResolver scriptBodyResolver;
    private final BodyResolver bodyResolver;
    private final ControlFlowAnalyzer controlFlowAnalyzer;
    private final DeclarationsChecker declarationsChecker;
    private final ModifiersChecker modifiersChecker;
    private final FunctionAnalyzerExtension functionAnalyzerExtension;
    private final DeclarationResolver declarationResolver;
    private final ImportsResolver importsResolver;
    private final OverloadResolver overloadResolver;
    private final OverrideResolver overrideResolver;
    private final TopDownAnalyzer topDownAnalyzer;
    private final TypeHierarchyResolver typeHierarchyResolver;
    private final ScriptHeaderResolver scriptHeaderResolver;
    private final VarianceChecker varianceChecker;
    private final LazyJavaPackageFragmentProvider lazyJavaPackageFragmentProvider;
    private final GlobalJavaResolverContext globalJavaResolverContext;
    private final DeserializedDescriptorResolver deserializedDescriptorResolver;
    private final JavaClassDataFinder javaClassDataFinder;
    private final BinaryClassAnnotationAndConstantLoaderImpl binaryClassAnnotationAndConstantLoader;

    public EclipseInjectorForTopDownAnalyzerForJvm(
        @NotNull Project project,
        @NotNull IJavaProject IJavaProject,
        @NotNull GlobalContext globalContext,
        @NotNull BindingTrace bindingTrace,
        @NotNull ModuleDescriptorImpl moduleDescriptor,
        @NotNull DeclarationProviderFactory declarationProviderFactory
    ) {
        this.project = project;
        this.IJavaProject = IJavaProject;
        this.globalContext = globalContext;
        this.storageManager = globalContext.getStorageManager();
        this.bindingTrace = bindingTrace;
        this.moduleDescriptor = moduleDescriptor;
        this.kotlinBuiltIns = moduleDescriptor.getBuiltIns();
        this.platformToKotlinClassMap = moduleDescriptor.getPlatformToKotlinClassMap();
        this.declarationProviderFactory = declarationProviderFactory;
        this.resolveSession = new ResolveSession(project, globalContext, getModuleDescriptor(), declarationProviderFactory, bindingTrace);
        this.lazyTopDownAnalyzer = new LazyTopDownAnalyzer();
        this.eclipseJavaClassFinder = new EclipseJavaClassFinder();
        this.virtualFileFinder = org.jetbrains.kotlin.load.kotlin.VirtualFileFinder.SERVICE.getInstance(project);
        this.traceBasedErrorReporter = new TraceBasedErrorReporter();
        this.deserializedDescriptorResolver = new DeserializedDescriptorResolver(traceBasedErrorReporter);
        this.eclipseExternalAnnotationResolver = new EclipseExternalAnnotationResolver();
        this.traceBasedExternalSignatureResolver = new TraceBasedExternalSignatureResolver();
        this.eclipseMethodSignatureChecker = new EclipseMethodSignatureChecker();
        this.eclipseTraceBasedJavaResolverCache = new EclipseTraceBasedJavaResolverCache();
        this.eclipseJavaPropertyInitializerEvaluator = new EclipseJavaPropertyInitializerEvaluator();
        this.samConversionResolver = SamConversionResolverImpl.INSTANCE$;
        this.eclipseJavaSourceElementFactory = new EclipseJavaSourceElementFactory();
        this.singleModuleClassResolver = new SingleModuleClassResolver();
        this.globalJavaResolverContext = new GlobalJavaResolverContext(storageManager, eclipseJavaClassFinder, virtualFileFinder, deserializedDescriptorResolver, eclipseExternalAnnotationResolver, traceBasedExternalSignatureResolver, traceBasedErrorReporter, eclipseMethodSignatureChecker, eclipseTraceBasedJavaResolverCache, eclipseJavaPropertyInitializerEvaluator, samConversionResolver, eclipseJavaSourceElementFactory, singleModuleClassResolver);
        this.lazyJavaPackageFragmentProvider = new LazyJavaPackageFragmentProvider(globalJavaResolverContext, getModuleDescriptor());
        this.javaDescriptorResolver = new JavaDescriptorResolver(lazyJavaPackageFragmentProvider, getModuleDescriptor());
        this.javaClassDataFinder = new JavaClassDataFinder(virtualFileFinder, deserializedDescriptorResolver);
        this.binaryClassAnnotationAndConstantLoader = new BinaryClassAnnotationAndConstantLoaderImpl(getModuleDescriptor(), storageManager, virtualFileFinder, traceBasedErrorReporter);
        this.deserializationComponentsForJava = new DeserializationComponentsForJava(storageManager, getModuleDescriptor(), javaClassDataFinder, binaryClassAnnotationAndConstantLoader, lazyJavaPackageFragmentProvider);
        this.additionalCheckerProvider = org.jetbrains.kotlin.load.kotlin.KotlinJvmCheckerProvider.INSTANCE$;
        this.mutablePackageFragmentProvider = new MutablePackageFragmentProvider(getModuleDescriptor());
        this.javaLazyAnalyzerPostConstruct = new JavaLazyAnalyzerPostConstruct();
        this.annotationResolver = new AnnotationResolver();
        this.callResolver = new CallResolver();
        this.argumentTypeResolver = new ArgumentTypeResolver();
        this.expressionTypingComponents = new ExpressionTypingComponents();
        this.expressionTypingServices = new ExpressionTypingServices(expressionTypingComponents);
        this.controlStructureTypingUtils = new ControlStructureTypingUtils(expressionTypingServices);
        this.dynamicTypesSettings = new DynamicTypesSettings();
        this.expressionTypingUtils = new ExpressionTypingUtils(expressionTypingServices, callResolver, kotlinBuiltIns);
        this.forLoopConventionsChecker = new ForLoopConventionsChecker();
        this.localClassifierAnalyzer = new LocalClassifierAnalyzer();
        this.reflectionTypes = new ReflectionTypes(getModuleDescriptor());
        this.callExpressionResolver = new CallExpressionResolver();
        this.descriptorResolver = new DescriptorResolver();
        this.delegatedPropertyResolver = new DelegatedPropertyResolver();
        this.qualifiedExpressionResolver = new QualifiedExpressionResolver();
        this.flexibleTypeCapabilitiesProvider = new FlexibleTypeCapabilitiesProvider();
        this.lazinessToken = new LazinessToken();
        this.typeResolver = new TypeResolver(annotationResolver, qualifiedExpressionResolver, getModuleDescriptor(), flexibleTypeCapabilitiesProvider, storageManager, lazinessToken, dynamicTypesSettings);
        this.partialBodyResolveProvider = new PartialBodyResolveProvider();
        this.candidateResolver = new CandidateResolver();
        this.callCompleter = new CallCompleter(argumentTypeResolver, candidateResolver);
        this.taskPrioritizer = new TaskPrioritizer(storageManager);
        this.jetImportsFactory = new JetImportsFactory();
        this.scopeProvider = new ScopeProvider(getResolveSession());
        this.additionalFileScopeProvider = new AdditionalFileScopeProvider();
        this.scriptBodyResolver = new ScriptBodyResolver();
        this.bodyResolver = new BodyResolver();
        this.controlFlowAnalyzer = new ControlFlowAnalyzer();
        this.declarationsChecker = new DeclarationsChecker();
        this.modifiersChecker = new ModifiersChecker(bindingTrace, additionalCheckerProvider);
        this.functionAnalyzerExtension = new FunctionAnalyzerExtension();
        this.declarationResolver = new DeclarationResolver();
        this.importsResolver = new ImportsResolver();
        this.overloadResolver = new OverloadResolver();
        this.overrideResolver = new OverrideResolver();
        this.topDownAnalyzer = new TopDownAnalyzer();
        this.typeHierarchyResolver = new TypeHierarchyResolver();
        this.scriptHeaderResolver = new ScriptHeaderResolver();
        this.varianceChecker = new VarianceChecker(bindingTrace);

        this.resolveSession.setAnnotationResolve(annotationResolver);
        this.resolveSession.setDescriptorResolver(descriptorResolver);
        this.resolveSession.setJetImportFactory(jetImportsFactory);
        this.resolveSession.setQualifiedExpressionResolver(qualifiedExpressionResolver);
        this.resolveSession.setScopeProvider(scopeProvider);
        this.resolveSession.setScriptBodyResolver(scriptBodyResolver);
        this.resolveSession.setTypeResolver(typeResolver);

        this.lazyTopDownAnalyzer.setBodyResolver(bodyResolver);
        this.lazyTopDownAnalyzer.setDeclarationResolver(declarationResolver);
        this.lazyTopDownAnalyzer.setKotlinCodeAnalyzer(resolveSession);
        this.lazyTopDownAnalyzer.setModuleDescriptor(moduleDescriptor);
        this.lazyTopDownAnalyzer.setOverloadResolver(overloadResolver);
        this.lazyTopDownAnalyzer.setOverrideResolver(overrideResolver);
        this.lazyTopDownAnalyzer.setTopDownAnalyzer(topDownAnalyzer);
        this.lazyTopDownAnalyzer.setTrace(bindingTrace);
        this.lazyTopDownAnalyzer.setVarianceChecker(varianceChecker);

        eclipseJavaClassFinder.setComponentPostConstruct(javaLazyAnalyzerPostConstruct);
        eclipseJavaClassFinder.setProjectScope(IJavaProject);

        traceBasedExternalSignatureResolver.setExternalAnnotationResolver(eclipseExternalAnnotationResolver);
        traceBasedExternalSignatureResolver.setProject(project);
        traceBasedExternalSignatureResolver.setTrace(bindingTrace);

        eclipseTraceBasedJavaResolverCache.setTrace(bindingTrace);

        traceBasedErrorReporter.setTrace(bindingTrace);

        javaLazyAnalyzerPostConstruct.setCodeAnalyzer(resolveSession);
        javaLazyAnalyzerPostConstruct.setProject(project);
        javaLazyAnalyzerPostConstruct.setTrace(bindingTrace);

        singleModuleClassResolver.setResolver(javaDescriptorResolver);

        annotationResolver.setCallResolver(callResolver);
        annotationResolver.setStorageManager(storageManager);
        annotationResolver.setTypeResolver(typeResolver);

        callResolver.setArgumentTypeResolver(argumentTypeResolver);
        callResolver.setCallCompleter(callCompleter);
        callResolver.setCandidateResolver(candidateResolver);
        callResolver.setExpressionTypingServices(expressionTypingServices);
        callResolver.setTaskPrioritizer(taskPrioritizer);
        callResolver.setTypeResolver(typeResolver);

        argumentTypeResolver.setBuiltIns(kotlinBuiltIns);
        argumentTypeResolver.setExpressionTypingServices(expressionTypingServices);
        argumentTypeResolver.setTypeResolver(typeResolver);

        expressionTypingServices.setAnnotationResolver(annotationResolver);
        expressionTypingServices.setBuiltIns(kotlinBuiltIns);
        expressionTypingServices.setCallExpressionResolver(callExpressionResolver);
        expressionTypingServices.setCallResolver(callResolver);
        expressionTypingServices.setDescriptorResolver(descriptorResolver);
        expressionTypingServices.setPartialBodyResolveProvider(partialBodyResolveProvider);
        expressionTypingServices.setProject(project);
        expressionTypingServices.setTypeResolver(typeResolver);

        expressionTypingComponents.setAdditionalCheckerProvider(additionalCheckerProvider);
        expressionTypingComponents.setBuiltIns(kotlinBuiltIns);
        expressionTypingComponents.setCallResolver(callResolver);
        expressionTypingComponents.setControlStructureTypingUtils(controlStructureTypingUtils);
        expressionTypingComponents.setDynamicTypesSettings(dynamicTypesSettings);
        expressionTypingComponents.setExpressionTypingServices(expressionTypingServices);
        expressionTypingComponents.setExpressionTypingUtils(expressionTypingUtils);
        expressionTypingComponents.setForLoopConventionsChecker(forLoopConventionsChecker);
        expressionTypingComponents.setGlobalContext(globalContext);
        expressionTypingComponents.setLocalClassifierAnalyzer(localClassifierAnalyzer);
        expressionTypingComponents.setPlatformToKotlinClassMap(platformToKotlinClassMap);
        expressionTypingComponents.setReflectionTypes(reflectionTypes);

        forLoopConventionsChecker.setBuiltIns(kotlinBuiltIns);
        forLoopConventionsChecker.setExpressionTypingServices(expressionTypingServices);
        forLoopConventionsChecker.setExpressionTypingUtils(expressionTypingUtils);
        forLoopConventionsChecker.setProject(project);

        callExpressionResolver.setExpressionTypingServices(expressionTypingServices);

        descriptorResolver.setAnnotationResolver(annotationResolver);
        descriptorResolver.setBuiltIns(kotlinBuiltIns);
        descriptorResolver.setDelegatedPropertyResolver(delegatedPropertyResolver);
        descriptorResolver.setExpressionTypingServices(expressionTypingServices);
        descriptorResolver.setStorageManager(storageManager);
        descriptorResolver.setTypeResolver(typeResolver);

        delegatedPropertyResolver.setBuiltIns(kotlinBuiltIns);
        delegatedPropertyResolver.setCallResolver(callResolver);
        delegatedPropertyResolver.setExpressionTypingServices(expressionTypingServices);

        candidateResolver.setArgumentTypeResolver(argumentTypeResolver);

        jetImportsFactory.setProject(project);

        scopeProvider.setAdditionalFileScopesProvider(additionalFileScopeProvider);

        scriptBodyResolver.setExpressionTypingServices(expressionTypingServices);

        bodyResolver.setAnnotationResolver(annotationResolver);
        bodyResolver.setCallResolver(callResolver);
        bodyResolver.setControlFlowAnalyzer(controlFlowAnalyzer);
        bodyResolver.setDeclarationsChecker(declarationsChecker);
        bodyResolver.setDelegatedPropertyResolver(delegatedPropertyResolver);
        bodyResolver.setExpressionTypingServices(expressionTypingServices);
        bodyResolver.setFunctionAnalyzerExtension(functionAnalyzerExtension);
        bodyResolver.setScriptBodyResolverResolver(scriptBodyResolver);
        bodyResolver.setTrace(bindingTrace);

        controlFlowAnalyzer.setTrace(bindingTrace);

        declarationsChecker.setDescriptorResolver(descriptorResolver);
        declarationsChecker.setModifiersChecker(modifiersChecker);
        declarationsChecker.setTrace(bindingTrace);

        functionAnalyzerExtension.setTrace(bindingTrace);

        declarationResolver.setAnnotationResolver(annotationResolver);
        declarationResolver.setDescriptorResolver(descriptorResolver);
        declarationResolver.setImportsResolver(importsResolver);
        declarationResolver.setTrace(bindingTrace);

        importsResolver.setImportsFactory(jetImportsFactory);
        importsResolver.setModuleDescriptor(moduleDescriptor);
        importsResolver.setQualifiedExpressionResolver(qualifiedExpressionResolver);
        importsResolver.setTrace(bindingTrace);

        overloadResolver.setTrace(bindingTrace);

        overrideResolver.setTrace(bindingTrace);

        topDownAnalyzer.setBodyResolver(bodyResolver);
        topDownAnalyzer.setDeclarationResolver(declarationResolver);
        topDownAnalyzer.setModuleDescriptor(moduleDescriptor);
        topDownAnalyzer.setOverloadResolver(overloadResolver);
        topDownAnalyzer.setOverrideResolver(overrideResolver);
        topDownAnalyzer.setPackageFragmentProvider(mutablePackageFragmentProvider);
        topDownAnalyzer.setTypeHierarchyResolver(typeHierarchyResolver);
        topDownAnalyzer.setVarianceChecker(varianceChecker);

        typeHierarchyResolver.setDescriptorResolver(descriptorResolver);
        typeHierarchyResolver.setImportsResolver(importsResolver);
        typeHierarchyResolver.setPackageFragmentProvider(mutablePackageFragmentProvider);
        typeHierarchyResolver.setScriptHeaderResolver(scriptHeaderResolver);
        typeHierarchyResolver.setTrace(bindingTrace);

        scriptHeaderResolver.setPackageFragmentProvider(mutablePackageFragmentProvider);
        scriptHeaderResolver.setTrace(bindingTrace);

        deserializedDescriptorResolver.setComponents(deserializationComponentsForJava);

        javaLazyAnalyzerPostConstruct.postCreate();

    }

    @PreDestroy
    public void destroy() {
    }

    public ModuleDescriptorImpl getModuleDescriptor() {
        return this.moduleDescriptor;
    }

    public ResolveSession getResolveSession() {
        return this.resolveSession;
    }

    public LazyTopDownAnalyzer getLazyTopDownAnalyzer() {
        return this.lazyTopDownAnalyzer;
    }

    public JavaDescriptorResolver getJavaDescriptorResolver() {
        return this.javaDescriptorResolver;
    }

    public DeserializationComponentsForJava getDeserializationComponentsForJava() {
        return this.deserializationComponentsForJava;
    }

}
