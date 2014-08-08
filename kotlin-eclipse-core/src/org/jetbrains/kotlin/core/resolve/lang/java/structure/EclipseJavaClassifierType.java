package org.jetbrains.kotlin.core.resolve.lang.java.structure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClassifier;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClassifierType;
import org.jetbrains.jet.lang.resolve.java.structure.JavaType;
import org.jetbrains.jet.lang.resolve.java.structure.JavaTypeParameter;
import org.jetbrains.jet.lang.resolve.java.structure.JavaTypeSubstitutor;
import org.jetbrains.jet.lang.resolve.java.structure.impl.JavaTypeSubstitutorImpl;

public class EclipseJavaClassifierType extends EclipseJavaType<ITypeBinding> implements JavaClassifierType {

    public EclipseJavaClassifierType(ITypeBinding typeBinding) {
        super(typeBinding);
    }

    @Override
    @Nullable
    public JavaClassifier getClassifier() {
        return EclipseJavaClassifier.create(getBinding().getTypeDeclaration());
    }

    @Override
    @NotNull
    public JavaTypeSubstitutor getSubstitutor() {
        JavaClassifier resolvedType = getClassifier();
        if (resolvedType instanceof JavaClass && getBinding().isParameterizedType()) {
            JavaClass javaClass = (JavaClass) resolvedType;
            List<JavaType> substitutedTypeArguments = getTypeArguments();
            
            int i = 0;
            Map<JavaTypeParameter, JavaType> substitutionMap = new HashMap<JavaTypeParameter, JavaType>();
            boolean isThisRawType = isRaw();
            for (JavaTypeParameter typeParameter : javaClass.getTypeParameters()) {
                substitutionMap.put(typeParameter, !isThisRawType ? substitutedTypeArguments.get(i) : null);
                i++;
            }
            
            return new JavaTypeSubstitutorImpl(substitutionMap);
        }
        
        return JavaTypeSubstitutor.EMPTY;
    }
    
    @Override
    @NotNull
    public Collection<JavaClassifierType> getSupertypes() {
        return EclipseJavaElementUtil.getSuperTypes(getBinding());
    }

    @Override
    @NotNull
    public String getPresentableText() {
        return getBinding().getQualifiedName();
    }

    @Override
    public boolean isRaw() {
        return getBinding().isRawType();
    }

    @Override
    @NotNull
    public List<JavaType> getTypeArguments() {
        List<JavaType> typeArguments = new ArrayList<JavaType>();
        for (ITypeBinding typeArgument : getBinding().getTypeArguments()) {
            typeArguments.add(EclipseJavaType.create(typeArgument));
        }
        
        return typeArguments;
    }

}
