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
package org.jetbrains.kotlin.core.resolve.lang.java;

import java.util.Arrays;

import javax.inject.Inject;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.internal.core.NameLookup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.JavaClassFinder;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass;
import org.jetbrains.jet.lang.resolve.java.structure.JavaPackage;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaClass;
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaPackage;

public class EclipseJavaClassFinder implements JavaClassFinder {

    private static ASTParser parser = ASTParser.newParser(AST.JLS4);
    
    private IJavaProject javaProject = null;
    
    @Inject
    public void setProjectScope(@NotNull IJavaProject project) {
        javaProject = project;
    }
    
    @Override
    @Nullable
    public JavaPackage findPackage(@NotNull FqName fqName) {
        IPackageFragment[] packageFragments = findPackageFragments(javaProject, fqName.asString(), false, false);
        if (packageFragments != null && packageFragments.length > 0) {
            return new EclipseJavaPackage(Arrays.asList(packageFragments));
        }

        return null;
    }
    
    @Override
    @Nullable
    public JavaClass findClass(@NotNull FqName fqName) {
        ITypeBinding typeBinding = findType(fqName, javaProject);
        if (typeBinding != null) {
            return new EclipseJavaClass(typeBinding);
        }
        
        return null;
    }
    
    @Nullable
    public static IPackageFragment[] findPackageFragments(IJavaProject javaProject, String name, 
            boolean partialMatch, boolean patternMatch) {
        try {
            NameLookup nameLookup = ((JavaProject) javaProject).newNameLookup((WorkingCopyOwner) null);
            return nameLookup.findPackageFragments(name, partialMatch, patternMatch);
        } catch (JavaModelException e) {
            KotlinLogger.logAndThrow(e);
        }
        
        return null;
    }
    
    @Nullable
    public static ITypeBinding findType(@NotNull FqName fqName, @NotNull IJavaProject javaProject) {
        IType eclipseType = null;
        try {
            eclipseType = javaProject.findType(fqName.asString());
        } catch (JavaModelException e) {
            KotlinLogger.logAndThrow(e);
        }
        
        if (eclipseType != null) {
            return createTypeBinding(eclipseType);
        }
        
        return null;
    }
    
    public static ITypeBinding createTypeBinding(IType type) {
        parser.setCompilerOptions(type.getJavaProject().getOptions(true));
        parser.setIgnoreMethodBodies(true);
        
        if (type.getCompilationUnit() != null) {
            parser.setSource(type.getCompilationUnit());
        } else { // class file with no source
            parser.setProject(type.getJavaProject());
            IBinding[] bindings = parser.createBindings(new IJavaElement[] { type }, null);
            if (bindings.length == 1 && bindings[0] instanceof ITypeBinding) {
                return (ITypeBinding) bindings[0];
            }
            
            return null;
        }
        
        parser.setResolveBindings(true);
        CompilationUnit root = (CompilationUnit) parser.createAST(null);
        return getTypeBinding(root, type);
    }
    
    private static ASTNode getParent(ASTNode node, Class<? extends ASTNode> parentClass) {
        do {
            node = node.getParent();
        } while (node != null && !parentClass.isInstance(node));
        return node;
    }
    
    private static ITypeBinding getTypeBinding(CompilationUnit root, IType type) {
        try {
            if (type.isAnonymous()) {
                final IJavaElement parent = type.getParent();
                if (parent instanceof IField && Flags.isEnum(((IMember) parent).getFlags())) {
                    final EnumConstantDeclaration constant = (EnumConstantDeclaration) NodeFinder.perform(root,
                            ((ISourceReference) parent).getSourceRange());
                    if (constant != null) {
                        final AnonymousClassDeclaration declaration = constant.getAnonymousClassDeclaration();
                        if (declaration != null) return declaration.resolveBinding();
                    }
                } else {
                    final ClassInstanceCreation creation = (ClassInstanceCreation) getParent(
                            NodeFinder.perform(root, type.getNameRange()), ClassInstanceCreation.class);
                    if (creation != null) return creation.resolveTypeBinding();
                }
            } else {
                final AbstractTypeDeclaration declaration = 
                        (AbstractTypeDeclaration) getParent(NodeFinder.perform(root, type.getNameRange()), AbstractTypeDeclaration.class);
                if (declaration != null) return declaration.resolveBinding();
            }
        } catch (JavaModelException e) {
            KotlinLogger.logAndThrow(e);
        }
        
        return null;
    }
}
