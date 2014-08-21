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
import org.eclipse.jdt.core.JavaCore;
import org.jetbrains.kotlin.core.utils.ProjectUtils;

public class KotlinClasspathContainer implements IClasspathContainer {
    
    public static final IPath CONTAINER_ID = new Path("org.jetbrains.kotlin.core.KOTLIN_CONTAINER");
    private static final String DESCRIPTION = "Kotlin Runtime Library";
    private static final String LIB_NAME = "kotlin-runtime";
    
    private static final IClasspathEntry KT_RUNTIME_CONTAINER_ENTRY = JavaCore.newContainerEntry(CONTAINER_ID);
    private static final IClasspathEntry[] ENTRIES = new IClasspathEntry[] {
        JavaCore.newLibraryEntry(
            new Path(ProjectUtils.buildLibPath(LIB_NAME)),
            null,
            null,
            true)
    };
    
    @Override
    public IClasspathEntry[] getClasspathEntries() {
        return ENTRIES;
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
