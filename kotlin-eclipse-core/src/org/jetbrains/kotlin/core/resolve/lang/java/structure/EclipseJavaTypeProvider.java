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

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.load.java.structure.JavaType;
import org.jetbrains.kotlin.load.java.structure.JavaTypeProvider;
import org.jetbrains.kotlin.load.java.structure.JavaWildcardType;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.resolve.lang.java.EclipseJavaClassFinder;

import com.intellij.psi.CommonClassNames;

public class EclipseJavaTypeProvider implements JavaTypeProvider {

    private final IJavaProject javaProject;
    
    public EclipseJavaTypeProvider(@NotNull IJavaProject javaProject) {
        this.javaProject = javaProject;
    }
    
    @Override
    @NotNull
    public JavaType createJavaLangObjectType() {
        try {
            IType type = javaProject.findType(CommonClassNames.JAVA_LANG_OBJECT);
            ITypeBinding typeBinding = EclipseJavaClassFinder.createTypeBinding(type);
            assert typeBinding != null : "Type binding for java.lang.Object can not be null";
            
            return EclipseJavaType.create(typeBinding);
        } catch (JavaModelException e) {
            KotlinLogger.logAndThrow(e);
            throw new IllegalStateException(e);
        }
        
    }

    @Override
    @NotNull
    public JavaWildcardType createUpperBoundWildcard(@NotNull JavaType bound) {
        return new EclipseJavaImmediateWildcardType(bound, true, this);
    }

    @Override
    @NotNull
    public JavaWildcardType createLowerBoundWildcard(@NotNull JavaType bound) {
        return new EclipseJavaImmediateWildcardType(bound, false, this);
    }

    @Override
    @NotNull
    public JavaWildcardType createUnboundedWildcard() {
        return new EclipseJavaImmediateWildcardType(null, false, this);
    }

}
