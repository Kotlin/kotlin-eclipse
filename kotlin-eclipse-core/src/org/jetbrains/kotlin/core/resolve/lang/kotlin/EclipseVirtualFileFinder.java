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
package org.jetbrains.kotlin.core.resolve.lang.kotlin;



import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.model.KotlinEnvironment;
import org.jetbrains.kotlin.core.resolve.lang.java.EclipseJavaClassFinder;
import org.jetbrains.kotlin.load.java.structure.JavaClass;
import org.jetbrains.kotlin.load.kotlin.JvmVirtualFileFinder;
import org.jetbrains.kotlin.load.kotlin.JvmVirtualFileFinderFactory;
import org.jetbrains.kotlin.load.kotlin.KotlinBinaryClassCache;
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass;
import org.jetbrains.kotlin.load.kotlin.VirtualFileKotlinClassFinder;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;

public class EclipseVirtualFileFinder extends VirtualFileKotlinClassFinder implements JvmVirtualFileFinderFactory {
    private final IJavaProject javaProject;
    
    public EclipseVirtualFileFinder(@NotNull IJavaProject javaProject) {
        this.javaProject = javaProject;
    }

    @Nullable
    @Override
    public VirtualFile findVirtualFileWithHeader(@NotNull ClassId classId) {
        try {
            IType type = javaProject.findType(classId.getPackageFqName().asString(), classId.getRelativeClassName().asString());
            if (type == null) return null;
            
            if (!type.isBinary()) return null;
            
            if (EclipseJavaClassFinder.isInKotlinBinFolder(type)) return null;
            
            IPath path;
            IResource resource = type.getResource();
            if (resource != null) {
                path = resource.getLocation(); // if resource exists then jar is in workspace
            } else {
                path = type.getPath();
            }
            if (path != null) {
                String relativePath = type.getFullyQualifiedName().replace('.', '/') + ".class";
                return KotlinEnvironment.getEnvironment(javaProject).getVirtualFileInJar(path, relativePath);
            }
        } catch (JavaModelException e) {
            KotlinLogger.logAndThrow(e);
        }
        
        return null;
    }
    
    @NotNull
    private static String classFileName(@NotNull JavaClass jClass) {
        JavaClass outerClass = jClass.getOuterClass();
        if (outerClass == null) return jClass.getName().asString();
        return classFileName(outerClass) + "$" + jClass.getName().asString();
    }
    
    @Override
    @Nullable
    public KotlinJvmBinaryClass findKotlinClass(@NotNull JavaClass javaClass) {
        FqName fqName = javaClass.getFqName();
        if (fqName == null) {
            return null;
        }
        ClassId classId = computeClassId(javaClass);
        if (classId == null) return null;
        
        VirtualFile file = findVirtualFileWithHeader(classId);
        if (file == null) {
            return null;
        }
        if (javaClass.getOuterClass() != null) {
            // For nested classes we get a file of the containing class, to get the actual class file for A.B.C,
            // we take the file for A, take its parent directory, then in this directory we look for A$B$C.class
            file = file.getParent().findChild(classFileName(javaClass) + ".class");
            assert file != null : "Virtual file not found for " + javaClass;
        }

        return KotlinBinaryClassCache.getKotlinBinaryClass(file);
    }
    
    @Nullable
    private static ClassId computeClassId(@NotNull JavaClass jClass) {
        JavaClass container = jClass.getOuterClass();
        if (container != null) {
            ClassId parentClassId = computeClassId(container);
            return parentClassId == null ? null : parentClassId.createNestedClassId(jClass.getName());
        }
        
        FqName fqName = jClass.getFqName();
        return fqName != null ? ClassId.topLevel(fqName) : null;
    }

    @Override
    @NotNull
    public JvmVirtualFileFinder create(@NotNull GlobalSearchScope scope) {
        return new EclipseVirtualFileFinder(javaProject);
    }
}
