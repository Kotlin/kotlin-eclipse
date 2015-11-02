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
package org.jetbrains.kotlin.core.utils;

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.KtParameter;
import org.jetbrains.kotlin.psi.KtTypeReference;

public class JetMainDetector {
    private JetMainDetector() {
    }

    public static boolean hasMain(@NotNull List<KtDeclaration> declarations) {
        return findMainFunction(declarations) != null;
    }

    public static boolean isMain(@NotNull KtNamedFunction function) {
        if ("main".equals(function.getName())) {
            List<KtParameter> parameters = function.getValueParameters();
            if (parameters.size() == 1) {
                KtTypeReference reference = parameters.get(0).getTypeReference();
                if (reference != null && reference.getText().equals("Array<String>")) {  // TODO correct check
                    return true;
                }
            }
        }
        
        return false;
    }

    @Nullable
    public static KtNamedFunction getMainFunction(@NotNull Collection<KtFile> files) {
        for (KtFile file : files) {
            KtNamedFunction mainFunction = findMainFunction(file.getDeclarations());
            if (mainFunction != null) {
                return mainFunction;
            }
        }
        
        return null;
    }

    @Nullable
    private static KtNamedFunction findMainFunction(@NotNull List<KtDeclaration> declarations) {
        for (KtDeclaration declaration : declarations) {
            if (declaration instanceof KtNamedFunction) {
                KtNamedFunction candidateFunction = (KtNamedFunction) declaration;
                if (isMain(candidateFunction)) {
                    return candidateFunction;
                }
            }
        }
        return null;
    }
}