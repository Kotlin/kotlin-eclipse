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

import com.intellij.mock.MockProject;
import org.eclipse.core.resources.IFolder;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.internal.core.NameLookup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.config.JvmTarget;
import org.jetbrains.kotlin.config.LanguageVersionSettings;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.model.KotlinEnvironment;
import org.jetbrains.kotlin.core.model.KotlinJavaManager;
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaClass;
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaPackage;
import org.jetbrains.kotlin.load.java.AbstractJavaClassFinder;
import org.jetbrains.kotlin.load.java.structure.JavaClass;
import org.jetbrains.kotlin.load.java.structure.JavaPackage;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.CodeAnalyzerInitializer;
import org.jetbrains.kotlin.resolve.jvm.JvmCodeAnalyzerInitializer;
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer;

import java.util.Arrays;
import java.util.Set;

public class EclipseJavaClassFinder extends AbstractJavaClassFinder {

    private IJavaProject javaProject;

    public EclipseJavaClassFinder(@NotNull IJavaProject project) {
        javaProject = project;
    }

    @Override
    public void initialize(@NotNull BindingTrace trace, @NotNull KotlinCodeAnalyzer codeAnalyzer, @NotNull LanguageVersionSettings languageVersionSettings, @NotNull JvmTarget jvmTarget) {
        if (javaProject == null) {
            return;
        }

        MockProject ideaProject = KotlinEnvironment.Companion.getEnvironment(javaProject.getProject()).getProject();
        JvmCodeAnalyzerInitializer tempInitializer = (JvmCodeAnalyzerInitializer) CodeAnalyzerInitializer.Companion.getInstance(ideaProject);
        //trace, codeAnalyzer.getModuleDescriptor(), codeAnalyzer, languageVersionSettings
        tempInitializer.initialize(trace, codeAnalyzer.getModuleDescriptor(), codeAnalyzer, languageVersionSettings, jvmTarget);
        //trace, codeAnalyzer.getModuleDescriptor(), codeAnalyzer
    }

    @Nullable
    @Override
    public JavaPackage findPackage(@NotNull FqName fqName, boolean b) {
        IPackageFragment[] packageFragments = findPackageFragments(javaProject, fqName.asString(), false, false);
        if (packageFragments != null && packageFragments.length > 0) {
            return new EclipseJavaPackage(Arrays.asList(packageFragments));
        }

        return null;
    }

    @Override
    @Nullable
    public JavaClass findClass(@NotNull Request request) {
        return findClass(request.getClassId());
    }

    @Override
    @Nullable
    public JavaClass findClass(@NotNull ClassId classId) {
        ITypeBinding typeBinding = findType(classId.asSingleFqName(), javaProject);
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
            return !isInKotlinBinFolder(eclipseType) ? createTypeBinding(eclipseType) : null;
        }
        
        return null;
    }
    
    public static boolean isInKotlinBinFolder(@NotNull IType eclipseType) {
        IFolder kotlinBinFolder = KotlinJavaManager.INSTANCE.getKotlinBinFolderFor(eclipseType.getJavaProject().getProject());
        IPackageFragmentRoot packageFragmentRoot = (IPackageFragmentRoot) eclipseType.getPackageFragment().getParent();
        return kotlinBinFolder.equals(packageFragmentRoot.getResource());
    }
    
    public static ITypeBinding createTypeBinding(IType type) {
        ASTParser parser = ASTParser.newParser(AST.JLS8);
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

    @Override
    @Nullable
    public Set<String> knownClassNamesInPackage(@NotNull FqName packageFqName) {
        // TODO Auto-generated method stub
        return null;
    }
}
