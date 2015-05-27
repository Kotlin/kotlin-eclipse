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



import kotlin.jvm.functions.Function2;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.jvm.compiler.JavaRoot;
import org.jetbrains.kotlin.cli.jvm.compiler.JavaRoot.RootType;
import org.jetbrains.kotlin.cli.jvm.compiler.JvmDependenciesIndex;
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
    @NotNull
    private final JvmDependenciesIndex index;

    public EclipseVirtualFileFinder(@NotNull JvmDependenciesIndex index) {
        this.index = index;
    }

    @Nullable
    @Override
    public VirtualFile findVirtualFileWithHeader(@NotNull ClassId classId) {
        // Copied from JvmCliVirtualFileFinder
        final String classFileName = classId.getRelativeClassName().asString().replace('.', '$');
        return index.findClass(classId, JavaRoot.OnlyBinary, new Function2<VirtualFile, JavaRoot.RootType, VirtualFile>() {
            @Override
            public VirtualFile invoke(VirtualFile dir, RootType rootType) {
                VirtualFile child = dir.findChild(classFileName + ".class");
                return (child != null && child.isValid()) ? child : null;
            }
        });
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
        VirtualFile file = findVirtualFileWithHeader(ClassId.topLevel(fqName));
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

    @Override
    @NotNull
    public JvmVirtualFileFinder create(@NotNull GlobalSearchScope scope) {
        return new EclipseVirtualFileFinder(index);
    }

}
