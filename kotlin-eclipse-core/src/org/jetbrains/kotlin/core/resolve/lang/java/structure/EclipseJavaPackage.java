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
package org.jetbrains.kotlin.core.resolve.lang.java.structure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.resolve.lang.java.EclipseJavaClassFinder;
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation;
import org.jetbrains.kotlin.load.java.structure.JavaClass;
import org.jetbrains.kotlin.load.java.structure.JavaElement;
import org.jetbrains.kotlin.load.java.structure.JavaPackage;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;

import kotlin.jvm.functions.Function1;

public class EclipseJavaPackage implements JavaElement, JavaPackage {
    
    private final List<IPackageFragment> packages = new ArrayList<>();
    private final IJavaProject javaProject;

    public EclipseJavaPackage(List<IPackageFragment> packages) {
        this.packages.addAll(packages);
        this.javaProject = packages.get(0).getJavaProject();
    }
    
    public EclipseJavaPackage(IPackageFragment pckg) {
        this(Collections.singletonList(pckg));
    }
    
    @Override
    @NotNull
    public Collection<JavaClass> getClasses(@NotNull Function1<? super Name, Boolean> nameFilter) {
        List<JavaClass> javaClasses = new ArrayList<>();
        for (IPackageFragment pckg : packages) {
            javaClasses.addAll(getClassesInPackage(pckg, nameFilter));
        }
        
        return javaClasses;
    }
    
    @Override
    @NotNull
    public Collection<JavaPackage> getSubPackages() {
        String thisPackageName = getFqName().asString();
        String pattern = thisPackageName.isEmpty() ? "*" : thisPackageName + ".";
        
        IPackageFragment[] packageFragments = EclipseJavaClassFinder.findPackageFragments(
                javaProject, pattern, true, true);

        int thisNestedLevel = thisPackageName.split("\\.").length;
        List<JavaPackage> javaPackages = new ArrayList<>();
        if (packageFragments != null && packageFragments.length > 0) {
            for (IPackageFragment packageFragment : packageFragments) {
                int subNestedLevel = packageFragment.getElementName().split("\\.").length;
                boolean applicableForRootPackage = thisNestedLevel == 1 && thisNestedLevel == subNestedLevel;
                if (!packageFragment.getElementName().isEmpty() && 
                   (applicableForRootPackage || (thisNestedLevel + 1 == subNestedLevel))) {
                    javaPackages.add(new EclipseJavaPackage(packageFragment));
                }
            }
        }
        
        return javaPackages;
    }

    @Override
    @NotNull
    public FqName getFqName() {
        return new FqName(packages.get(0).getElementName()); // They all should have same names
    }
    
    private List<JavaClass> getClassesInPackage(IPackageFragment javaPackage, Function1<? super Name, ? extends Boolean> nameFilter) {
        try {
            List<JavaClass> javaClasses = new ArrayList<>();
            for (IClassFile classFile : javaPackage.getClassFiles()) {
                IType type = classFile.getType();
                if (isOuterClass(classFile)) {
                    String elementName = type.getElementName();
                    if (Name.isValidIdentifier(elementName) && nameFilter.invoke(Name.identifier(elementName))) {
                        javaClasses.add(new EclipseOptimizedJavaClass(type));
                    }
                }
            }
            
            for (ICompilationUnit cu : javaPackage.getCompilationUnits()) {
                for (IType javaClass : cu.getAllTypes()) {
                    String elementName = javaClass.getElementName();
                    if (Name.isValidIdentifier(elementName) && nameFilter.invoke(Name.identifier(elementName))) {
                        javaClasses.add(new EclipseOptimizedJavaClass(javaClass));
                    }
                }
            }
            
            return javaClasses;
        } catch (JavaModelException e) {
            KotlinLogger.logAndThrow(e);
            throw new IllegalStateException(e);
        }
    }

    // TODO: Add correct resolve binding for all class files with $
    private boolean isOuterClass(IClassFile classFile) {
        return !classFile.getElementName().contains("$");
    }

    @Override
    @Nullable
    public JavaAnnotation findAnnotation(@NotNull FqName arg0) {
        return null;
    }

    @Override
    @NotNull
    public Collection<JavaAnnotation> getAnnotations() {
        return Collections.emptyList();
    }

    @Override
    public boolean isDeprecatedInJavaDoc() {
        return false;
    }
}
