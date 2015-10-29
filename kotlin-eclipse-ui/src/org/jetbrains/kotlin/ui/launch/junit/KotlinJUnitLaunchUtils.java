/*******************************************************************************
 * Copyright 2010-2014 JetBrains s.r.o.
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
 *******************************************************************************/
package org.jetbrains.kotlin.ui.launch.junit;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.model.KotlinJavaManager;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtFile;

public class KotlinJUnitLaunchUtils {
    @Nullable
    public static KtClass getSingleJetClass(@NotNull IFile file) {
        if (!KotlinPsiManager.INSTANCE.exists(file)) return null;
        
        KtFile jetFile = KotlinPsiManager.INSTANCE.getParsedFile(file);
        
        KtClass jetClass = null;
        for (KtDeclaration declaration : jetFile.getDeclarations()) {
            if (declaration instanceof KtClass) {
                if (jetClass != null) {
                    return null;
                } else {
                    jetClass = (KtClass) declaration;
                }
            }
        }
        
        return jetClass;
    }
    
    @Nullable
    public static IType getEclipseTypeForSingleClass(@NotNull IFile file) {
        KtClass jetClass = getSingleJetClass(file);
        return jetClass != null ? KotlinJavaManager.INSTANCE$.findEclipseType(jetClass, JavaCore.create(file.getProject())) : null;
    }    
}
