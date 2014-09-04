/*******************************************************************************
 * Copyright 2000-2014 JetBrains s.r.o.
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
package org.jetbrains.kotlin.core.resolve;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import kotlin.Function1;

import org.eclipse.jdt.core.IJavaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.analyzer.ModuleContent;
import org.jetbrains.jet.analyzer.ModuleInfo;
import org.jetbrains.jet.context.ContextPackage;
import org.jetbrains.jet.lang.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.java.JvmPlatformParameters;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.kotlin.core.utils.KotlinEnvironment;
import org.jetbrains.kotlin.core.utils.ProjectUtils;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;

public class KotlinAnalyzer {

    @NotNull
    public static ResolveSession getLazyResolveSession(@NotNull IJavaProject javaProject) {
        Project project = KotlinEnvironment.getEnvironment(javaProject).getProject();
        final TestModule module = new TestModule();
        return EclipseAnalyzerFacadeForJVM.INSTANCE.createLazyResolveSession(
                javaProject,
                project, 
                ContextPackage.GlobalContext(), 
                module, 
                new ModuleContent(Collections.<JetFile>emptyList(), GlobalSearchScope.allScope(project)), 
                new JvmPlatformParameters(new Function1<JavaClass, ModuleInfo>() {
                    @Override
                    public ModuleInfo invoke(JavaClass javaClass) {
                        return module;
                    }
                }));
    }
    
    private static class TestModule implements ModuleInfo {
        @Override
        @NotNull
        public List<ModuleInfo> dependencies() {
            return Collections.<ModuleInfo>singletonList(this);
        }
        
        @Override
        @NotNull
        public DependencyOnBuiltins dependencyOnBuiltins() {
            return ModuleInfo.DependenciesOnBuiltins.LAST;
        }
        
        @Override
        @NotNull
        public Name getName() {
            return Name.special("<Module for lazy resolve");
        }
        
        @Override
        @NotNull
        public Collection<ModuleInfo> friends() {
            return Collections.emptyList();
        }
    }
    
    @NotNull
    public static AnalyzeExhaust analyzeDeclarations(@NotNull IJavaProject javaProject) {
        return analyzeProject(javaProject, Predicates.<PsiFile>alwaysFalse());
    }
    
    @NotNull
    public static AnalyzeExhaust analyzeWholeProject(@NotNull IJavaProject javaProject) {
        return analyzeProject(javaProject, Predicates.<PsiFile>alwaysTrue());
    }

    @NotNull
    public static AnalyzeExhaust analyzeOneFileCompletely(@NotNull IJavaProject javaProject, @NotNull PsiFile psiFile) {
        return analyzeProject(javaProject, Predicates.equalTo(psiFile));
    }
    
    private static AnalyzeExhaust analyzeProject(@NotNull IJavaProject javaProject, @NotNull Predicate<PsiFile> filesToAnalyzeCompletely) {
        KotlinEnvironment kotlinEnvironment = KotlinEnvironment.getEnvironment(javaProject);
        return analyzeExhaustProject(javaProject, kotlinEnvironment, filesToAnalyzeCompletely);
    }
    
    @NotNull
    private static AnalyzeExhaust analyzeExhaustProject(@NotNull IJavaProject javaProject, @NotNull KotlinEnvironment kotlinEnvironment, 
            @NotNull Predicate<PsiFile> filesToAnalyzeCompletely) {
        ModuleDescriptorImpl module = EclipseAnalyzerFacadeForJVM.createJavaModule("<module>");
        module.addDependencyOnModule(module);
        module.addDependencyOnModule(KotlinBuiltIns.getInstance().getBuiltInsModule());
        module.seal();
        
        return EclipseAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                javaProject, 
                kotlinEnvironment.getProject(), 
                ProjectUtils.getSourceFilesWithDependencies(javaProject), 
                new BindingTraceContext(), 
                filesToAnalyzeCompletely, 
                module);
    }
}