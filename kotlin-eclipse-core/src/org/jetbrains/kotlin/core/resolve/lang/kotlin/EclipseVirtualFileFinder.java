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

import java.io.File;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.jvm.compiler.ClassPath;
import org.jetbrains.kotlin.core.filesystem.KotlinLightClassManager;
import org.jetbrains.kotlin.load.java.structure.JavaClass;
import org.jetbrains.kotlin.load.kotlin.KotlinBinaryClassCache;
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass;
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder;
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory;
import org.jetbrains.kotlin.load.kotlin.VirtualFileKotlinClassFinder;
import org.jetbrains.kotlin.name.FqName;

import com.intellij.ide.highlighter.JavaClassFileType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;

public class EclipseVirtualFileFinder extends VirtualFileKotlinClassFinder implements VirtualFileFinderFactory {
    @NotNull
    private final ClassPath classPath;

    public EclipseVirtualFileFinder(@NotNull ClassPath path) {
        classPath = path;
    }

    @Nullable
    @Override
    public VirtualFile findVirtualFileWithHeader(@NotNull FqName className) {
        for (VirtualFile root : classPath) {
            VirtualFile fileInRoot = findFileInRoot(className.asString(), root, '.');
            //NOTE: currently we use VirtualFileFinder to find Kotlin binaries only
            if (fileInRoot != null) {
                if (!isKotlinLightClass(fileInRoot.getPath()) && KotlinBinaryClassCache.getKotlinBinaryClass(fileInRoot) != null) {
                    return fileInRoot;
                }
            }
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
        VirtualFile file = findVirtualFileWithHeader(fqName);
        if (file == null) {
            return null;
        }
        if (javaClass.getOuterClass() != null) {
            // For nested classes we get a file of the containing class, to get the actual class file for A.B.C,
            // we take the file for A, take its parent directory, then in this directory we look for A$B$C.class
            file = file.getParent().findChild(classFileName(javaClass) + ".class");
            assert file != null : "Virtual file not found for " + javaClass;
        }

        if (file.getFileType() != JavaClassFileType.INSTANCE) return null;

        return KotlinBinaryClassCache.getKotlinBinaryClass(file);
    }

    @Override
    public VirtualFile findVirtualFile(@NotNull String internalName) {
        for (VirtualFile root : classPath) {
            VirtualFile fileInRoot = findFileInRoot(internalName, root, '/');
            if (fileInRoot != null) {
                return fileInRoot;
            }
        }
        return null;
    }
    
    private static boolean isKotlinLightClass(@NotNull String fullPath) {
        return KotlinLightClassManager.INSTANCE.isLightClass(new File(fullPath));
    }

    //NOTE: copied with some changes from CoreJavaFileManager
    @Nullable
    private static VirtualFile findFileInRoot(@NotNull String qName, @NotNull VirtualFile root, char separator) {
        String pathRest = qName;
        VirtualFile cur = root;

        while (true) {
            int dot = pathRest.indexOf(separator);
            if (dot < 0) break;

            String pathComponent = pathRest.substring(0, dot);
            VirtualFile child = cur.findChild(pathComponent);

            if (child == null) break;
            pathRest = pathRest.substring(dot + 1);
            cur = child;
        }

        String className = pathRest.replace('.', '$');
        VirtualFile vFile = cur.findChild(className + ".class");
        if (vFile != null) {
            if (!vFile.isValid()) {
                //TODO: log
                return null;
            }
            return vFile;
        }
        return null;
    }

    @Override
    @NotNull
    public VirtualFileFinder create(@NotNull GlobalSearchScope scope) {
        return new EclipseVirtualFileFinder(classPath);
    }

}
