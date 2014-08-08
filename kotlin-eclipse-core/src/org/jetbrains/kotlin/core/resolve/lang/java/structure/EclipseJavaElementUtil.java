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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.Visibilities;
import org.jetbrains.jet.lang.descriptors.Visibility;
import org.jetbrains.jet.lang.resolve.java.JavaVisibilities;
import org.jetbrains.jet.lang.resolve.java.structure.JavaAnnotation;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClassifierType;
import org.jetbrains.jet.lang.resolve.name.FqName;

import com.google.common.collect.Lists;

public class EclipseJavaElementUtil {
    
    @NotNull
    static Visibility getVisibility(@NotNull IBinding member) {
        int flags = member.getModifiers();
        if (Modifier.isPublic(flags)) {
            return Visibilities.PUBLIC;
        } else if (Modifier.isPrivate(flags)) {
            return Visibilities.PRIVATE;
        } else if (Modifier.isProtected(flags)) {
            return Flags.isStatic(flags) ? JavaVisibilities.PROTECTED_STATIC_VISIBILITY : JavaVisibilities.PROTECTED_AND_PACKAGE;
        }
        
        return JavaVisibilities.PACKAGE_VISIBILITY;
    }

    private static List<ITypeBinding> getEclipseSuperTypes(@NotNull ITypeBinding typeBinding) {
        List<ITypeBinding> superTypes = Lists.newArrayList();
        for (ITypeBinding superInterface : typeBinding.getInterfaces()) {
            superTypes.add(superInterface);
        }
        
        ITypeBinding superClass = typeBinding.getSuperclass();
        if (superClass != null) {
            superTypes.add(superClass);
        }
        
        return superTypes;
    }
    
    static List<JavaClassifierType> getSuperTypes(@NotNull ITypeBinding typeBinding) {
        List<ITypeBinding> eclipseSuperTypes = getEclipseSuperTypes(typeBinding);
        List<JavaClassifierType> javaSuperTypes = Lists.newArrayList();
        for (ITypeBinding eclipseSuperType : eclipseSuperTypes) {
            javaSuperTypes.add(new EclipseJavaClassifierType(eclipseSuperType));
        }
        
        return javaSuperTypes;
    }
    
    @NotNull
    static Collection<JavaAnnotation> getAnnotations(@NotNull IBinding binding) {
        return convertAnnotationBindings(binding.getAnnotations());
    }

    @Nullable
    static JavaAnnotation findAnnotation(@NotNull IBinding binding, @NotNull FqName fqName) {
        return findAnnotationIn(binding.getAnnotations(), fqName);
    }
    
    static JavaAnnotation findAnnotationIn(@NotNull IAnnotationBinding[] annotationBindings, @NotNull FqName fqName) {
        for (IAnnotationBinding annotation : annotationBindings) {
            String annotationFQName = annotation.getAnnotationType().getQualifiedName();
            if (fqName.asString().equals(annotationFQName)) {
                return new EclipseJavaAnnotation(annotation);
            }
        }
        
        return null;
    }
    
    static Collection<JavaAnnotation> convertAnnotationBindings(@NotNull IAnnotationBinding[] annotationBindings) {
        List<JavaAnnotation> annotations = new ArrayList<JavaAnnotation>();
        for (IAnnotationBinding annotation : annotationBindings) {
            annotations.add(new EclipseJavaAnnotation(annotation));
        }
        
        return annotations;
    }
}
