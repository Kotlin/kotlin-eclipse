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
package org.jetbrains.kotlin.generators.injectors;

import java.io.IOException;

import org.eclipse.jdt.core.IJavaProject;
import org.jetbrains.jet.context.GlobalContext;
import org.jetbrains.jet.di.DependencyInjectorGenerator;
import org.jetbrains.jet.di.DiType;
import org.jetbrains.jet.di.Expression;
import org.jetbrains.jet.di.GivenExpression;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzer;
import org.jetbrains.jet.di.InjectorGeneratorUtil;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.LazyTopDownAnalyzer;
import org.jetbrains.jet.lang.resolve.MutablePackageFragmentProvider;
import org.jetbrains.jet.lang.resolve.TopDownAnalyzer;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.java.resolver.TraceBasedErrorReporter;
import org.jetbrains.jet.lang.resolve.java.resolver.TraceBasedExternalSignatureResolver;
import org.jetbrains.jet.lang.resolve.kotlin.DeserializationGlobalContextForJava;
import org.jetbrains.jet.lang.resolve.kotlin.VirtualFileFinder;
import org.jetbrains.kotlin.core.resolve.lang.java.EclipseJavaClassFinder;
import org.jetbrains.kotlin.core.resolve.lang.java.resolver.EclipseExternalAnnotationResolver;
import org.jetbrains.kotlin.core.resolve.lang.java.resolver.EclipseJavaSourceElementFactory;
import org.jetbrains.kotlin.core.resolve.lang.java.resolver.EclipseMethodSignatureChecker;
import org.jetbrains.kotlin.core.resolve.lang.java.resolver.EclipseTraceBasedJavaResolverCache;
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaPropertyInitializerEvaluator;

import com.intellij.openapi.project.Project;

public class InjectorsGenerator {
    
    private final DependencyInjectorGenerator generator;
    
    private InjectorsGenerator(String targetSourceRoot, String injectorPackageName, 
            String injectorClassName) {
        generator = new DependencyInjectorGenerator();
        generator.configure(targetSourceRoot, injectorPackageName, injectorClassName, "org.jetbrains.kotlin.core.injectors.InjectorsGenerator");
    }
    
    public static InjectorsGenerator generatorForTopDownAnalyzerForJvm() {
    	InjectorsGenerator injectorsGenerator = new InjectorsGenerator("../kotlin-eclipse-core/src", 
    			"org.jetbrains.kotlin.core.injectors", "EclipseInjectorForTopDownAnalyzerForJvm");
    	injectorsGenerator.configureGeneratorForTopDownAnalyzerForJvm();
    	
    	return injectorsGenerator;
    }
    
    public void generate() throws IOException {
    	generator.generate();
    }
    
    private void configureGeneratorForTopDownAnalyzerForJvm() {
    	generator.implementInterface(InjectorForTopDownAnalyzer.class);
        
        addParameter(Project.class, false);
        addParameter(GlobalContext.class, true);
        addParameter(BindingTrace.class, false);
        addPublicParameter(ModuleDescriptor.class, true);
        addParameter(IJavaProject.class, false);
        
        addPublicField(TopDownAnalyzer.class);
        addPublicField(LazyTopDownAnalyzer.class);
        
        addField(MutablePackageFragmentProvider.class);
        
        addPublicField(JavaDescriptorResolver.class);
        addPublicField(DeserializationGlobalContextForJava.class);
        
        addFields(
                EclipseJavaClassFinder.class, 
                TraceBasedExternalSignatureResolver.class, 
                EclipseTraceBasedJavaResolverCache.class, 
                TraceBasedErrorReporter.class,
                EclipseMethodSignatureChecker.class,
                EclipseExternalAnnotationResolver.class,
                MutablePackageFragmentProvider.class,
                EclipseJavaPropertyInitializerEvaluator.class,
                EclipseJavaSourceElementFactory.class);
        
        addField(VirtualFileFinder.class, 
                new GivenExpression(VirtualFileFinder.class.getName() + ".SERVICE.getInstance(project)"));
    }
    
    private void addPublicField(Class<?> fieldType) {
        generator.addField(true, new DiType(fieldType), getDefaultName(fieldType), null, false);
    }
    
    private void addField(Class<?> fieldType) {
        generator.addField(false, new DiType(fieldType), getDefaultName(fieldType), null, false);
    }
    
    private void addField(Class<?> fieldType, Expression init) {
        generator.addField(false, new DiType(fieldType), getDefaultName(fieldType), init, false);
    }
    
    private void addFields(Class<?>... fieldsTypes) {
        for (Class<?> field : fieldsTypes) {
            addField(field);
        }
    }
    
    private void addParameter(Class<?> parameterType, boolean useAsContext) {
        generator.addParameter(false, new DiType(parameterType), getDefaultName(parameterType), true, useAsContext);
    }
    
    private void addPublicParameter(Class<?> parameterType, boolean useAsContext) {
        generator.addParameter(true, new DiType(parameterType), getDefaultName(parameterType), true, useAsContext);
    }
    
    private String getDefaultName(Class<?> entityType) {
        return InjectorGeneratorUtil.var(new DiType(entityType));
    }
}
