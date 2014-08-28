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

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.internal.compiler.env.IBinaryType;
import org.eclipse.jdt.internal.core.BinaryType;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.Visibilities;
import org.jetbrains.jet.lang.descriptors.Visibility;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.java.JavaVisibilities;
import org.jetbrains.jet.lang.resolve.java.structure.JavaAnnotation;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.resolve.lang.java.EclipseJavaClassFinder;

import com.google.common.collect.Lists;
import com.intellij.psi.CommonClassNames;

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
    
    static ITypeBinding[] getSuperTypes(@NotNull ITypeBinding typeBinding) {
        List<ITypeBinding> superTypes = Lists.newArrayList();
        for (ITypeBinding superInterface : typeBinding.getInterfaces()) {
            superTypes.add(superInterface);
        }
        
        ITypeBinding superClass = typeBinding.getSuperclass();
        if (superClass != null) {
            superTypes.add(superClass);
        }
        
        return superTypes.toArray(new ITypeBinding[superTypes.size()]);
    }
    
    static List<ITypeBinding> getAllSuperTypesWithObject(@NotNull ITypeBinding typeBinding) {
        List<ITypeBinding> allSuperTypes = Lists.newArrayList();
        
        boolean javaLangObjectInSuperTypes = false;
        for (ITypeBinding superType : Bindings.getAllSuperTypes(typeBinding)) {
            if (superType.getQualifiedName().equals(CommonClassNames.JAVA_LANG_OBJECT)) {
                javaLangObjectInSuperTypes = true;
            }
            allSuperTypes.add(superType);
        }
        
        if (!javaLangObjectInSuperTypes) {
            allSuperTypes.add(getJavaLangObjectBinding(typeBinding.getJavaElement().getJavaProject()));
        }
        
        return allSuperTypes;
    }
    
    @NotNull
    private static ITypeBinding getJavaLangObjectBinding(@NotNull IJavaProject javaProject) {
        try {
            IType javaType = javaProject.findType(CommonClassNames.JAVA_LANG_OBJECT);
            return EclipseJavaClassFinder.createTypeBinding(javaType);
        } catch (JavaModelException e) {
            KotlinLogger.logAndThrow(e);
            throw new IllegalStateException(e);
        }
    }
    
    static JavaAnnotation findAnnotation(@NotNull IAnnotationBinding[] annotationBindings, @NotNull FqName fqName) {
        for (IAnnotationBinding annotation : annotationBindings) {
            String annotationFQName = annotation.getAnnotationType().getQualifiedName();
            if (fqName.asString().equals(annotationFQName)) {
                return new EclipseJavaAnnotation(annotation);
            }
        }
        
        return null;
    }
    
    public static boolean isKotlinLightClass(@NotNull BinaryType binaryType) {
        try {
            IBinaryType rawBinaryType = (IBinaryType) ((binaryType).getElementInfo());
            return getKotlinFileIfExist(binaryType.getSourceFileName(rawBinaryType)) != null;
        } catch (JavaModelException e) {
            KotlinLogger.logAndThrow(e);
        }
        
        return false;
    }
    
    @Nullable
    public static JetFile getKotlinFileIfExist(@NotNull String sourceFileName) {
        IPath sourceFilePath = new Path(sourceFileName);
        IFile projectFile = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(sourceFilePath);
        if (KotlinPsiManager.INSTANCE.exists(projectFile)) {
            return (JetFile) KotlinPsiManager.INSTANCE.getParsedFile(projectFile);
        }
        
        return null;
    }
}
