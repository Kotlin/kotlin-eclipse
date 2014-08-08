package org.jetbrains.kotlin.core.resolve.lang.java.structure;

import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClassifierType;
import org.jetbrains.jet.lang.resolve.java.structure.JavaType;
import org.jetbrains.jet.lang.resolve.java.structure.JavaTypeParameter;
import org.jetbrains.jet.lang.resolve.java.structure.JavaTypeParameterListOwner;
import org.jetbrains.jet.lang.resolve.java.structure.JavaTypeProvider;
import org.jetbrains.jet.lang.resolve.name.Name;

import com.google.common.collect.Lists;

public class EclipseJavaTypeParameter extends EclipseJavaClassifier<ITypeBinding> implements JavaTypeParameter {

    protected EclipseJavaTypeParameter(ITypeBinding binding) {
        super(binding);
    }

    @Override
    @NotNull
    public Name getName() {
        return Name.identifier(getBinding().getName());
    }

    @Override
    public int getIndex() {
        JavaTypeParameterListOwner owner = getOwner();
        if (owner == null) {
            return 0;
        }
        
        int typeParameterNum = 0;
        for (JavaTypeParameter ownerParameter : owner.getTypeParameters()) {
            if (ownerParameter.equals(this)) {
                return typeParameterNum;
            }
            typeParameterNum++;
        }
        
        return -1;
    }

    @Override
    @NotNull
    public Collection<JavaClassifierType> getUpperBounds() {
        List<JavaClassifierType> bounds = Lists.newArrayList();
        for (ITypeBinding bound : getBinding().getTypeBounds()) {
            bounds.add(new EclipseJavaClassifierType(bound));
        }
        
        return bounds;
    }

    @Override
    @Nullable
    public JavaTypeParameterListOwner getOwner() {
        IMethodBinding methodOwner = getBinding().getDeclaringMethod();
        if (methodOwner != null) {
            return new EclipseJavaMethod(methodOwner);
        }
        
        ITypeBinding typeOwner = getBinding().getDeclaringClass();
        if (typeOwner != null) {
            return new EclipseJavaClass(typeOwner);
        }
        
        return null;
    }

    @Override
    @NotNull
    public JavaType getType() {
        return EclipseJavaType.create(getBinding().getTypeDeclaration());
    }

    @Override
    @NotNull
    public JavaTypeProvider getTypeProvider() {
        return new EclipseJavaTypeProvider(getBinding().getJavaElement().getJavaProject());
    }

}
