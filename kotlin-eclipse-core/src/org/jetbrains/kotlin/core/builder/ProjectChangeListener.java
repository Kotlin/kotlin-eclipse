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
package org.jetbrains.kotlin.core.builder;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;

public class ProjectChangeListener implements IResourceDeltaVisitor {

    @Override
    public boolean visit(IResourceDelta delta) throws CoreException {
        IResource resource = delta.getResource();
        if (resource instanceof IFile) {
            IFile file = (IFile) resource;
            if (KotlinPsiManager.INSTANCE.isCompatibleResource(file)) {
                if (delta.getKind() != IResourceDelta.CHANGED) {
                    KotlinPsiManager.INSTANCE.updateProjectPsiSources(file, delta.getKind());
                }
            }
        } else if (resource instanceof IProject) {
            KotlinPsiManager.INSTANCE.updateProjectPsiSources((IProject) resource, delta.getKind());
        }
        
        return true;
    }
}