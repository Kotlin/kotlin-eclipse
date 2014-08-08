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
package org.jetbrains.kotlin.core.resolve.lang.java.structure;

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClassifier;

public abstract class EclipseJavaClassifier<T extends ITypeBinding> extends EclipseJavaElement<T> implements JavaClassifier {
    public EclipseJavaClassifier(@NotNull T javaType) {
        super(javaType);
    }
    
    static JavaClassifier create(@NotNull ITypeBinding element) {
        if (element.isTypeVariable()) {
            return new EclipseJavaTypeParameter(element);
        } else if (element.isClass() || element.isParameterizedType() || element.isInterface() ||
                element.isEnum()) {
            return new EclipseJavaClass(element);
        } else {
            throw new IllegalArgumentException("Element: " + element.getName() + " is not JavaClassifier");
        }
    }
}
