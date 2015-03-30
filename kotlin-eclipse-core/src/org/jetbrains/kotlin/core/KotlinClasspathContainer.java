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
package org.jetbrains.kotlin.core;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.jetbrains.kotlin.core.model.KotlinJavaManager;
import org.jetbrains.kotlin.core.utils.ProjectUtils;

public class KotlinClasspathContainer implements IClasspathContainer {
    private final IJavaProject javaProject;
    
    public KotlinClasspathContainer(IJavaProject javaProject) {
        this.javaProject = javaProject;
    }
    
    public static final IPath CONTAINER_ID = new Path("org.jetbrains.kotlin.core.KOTLIN_CONTAINER");
    private static final String DESCRIPTION = "Kotlin Runtime Library";
    private static final String LIB_NAME = "kotlin-runtime";
    
    private static final IClasspathEntry KT_RUNTIME_CONTAINER_ENTRY = JavaCore.newContainerEntry(CONTAINER_ID);
    
    @Override
    public IClasspathEntry[] getClasspathEntries() {
        IClasspathEntry kotlinRuntimeEntry = JavaCore.newLibraryEntry(
                new Path(ProjectUtils.buildLibPath(LIB_NAME)),
                null,
                null,
                true);
        
        IClasspathEntry kotlinBinFolderEntry = JavaCore.newLibraryEntry(
                new Path(javaProject.getProject().getName()).append(KotlinJavaManager.KOTLIN_BIN_FOLDER).makeAbsolute(), 
                null, null, true);
        
        return new IClasspathEntry[] { kotlinRuntimeEntry, kotlinBinFolderEntry };
    }
    
    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
    
    @Override
    public int getKind() {
        return K_APPLICATION;
    }
    
    @Override
    public IPath getPath() {
        return CONTAINER_ID;
    }
    
    public static IClasspathEntry getKotlinRuntimeContainerEntry() {
        return KT_RUNTIME_CONTAINER_ENTRY;
    }
}
