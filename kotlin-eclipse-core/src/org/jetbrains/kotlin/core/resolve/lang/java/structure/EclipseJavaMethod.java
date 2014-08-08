package org.jetbrains.kotlin.core.resolve.lang.java.structure;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass;
import org.jetbrains.jet.lang.resolve.java.structure.JavaMethod;
import org.jetbrains.jet.lang.resolve.java.structure.JavaType;
import org.jetbrains.jet.lang.resolve.java.structure.JavaTypeParameter;
import org.jetbrains.jet.lang.resolve.java.structure.JavaValueParameter;

public class EclipseJavaMethod extends EclipseJavaMember<IMethodBinding> implements JavaMethod  {

    protected EclipseJavaMethod(IMethodBinding method) {
        super(method);
    }

    @Override
    @NotNull
    public List<JavaTypeParameter> getTypeParameters() {
        List<JavaTypeParameter> typeParameters = new ArrayList<JavaTypeParameter>();
        for (ITypeBinding typeParameter : getBinding().getTypeParameters()) {
            typeParameters.add(new EclipseJavaTypeParameter(typeParameter));
        }
        
        return typeParameters;
    }

    @Override
    @NotNull
    public List<JavaValueParameter> getValueParameters() {
        List<JavaValueParameter> parameters = new ArrayList<JavaValueParameter>();
        ITypeBinding[] parameterTypes = getBinding().getParameterTypes();
        
        int parameterTypesCount = parameterTypes.length;
        for (int i = 0; i < parameterTypesCount; ++i) {
            if (i < parameterTypesCount - 1) {
                parameters.add(new EclipseJavaValueParameter(
                        parameterTypes[i], 
                        getBinding().getParameterAnnotations(i),
                        "arg" + i, 
                        false));
            } else {
                parameters.add(new EclipseJavaValueParameter(
                        parameterTypes[i],
                        getBinding().getParameterAnnotations(i),
                        "arg" + i, 
                        isVararg()));
            }
        }
        
        return parameters;
    }

    @Override
    public boolean hasAnnotationParameterDefaultValue() {
        return getBinding().getDefaultValue() != null;
    }

    @Override
    @Nullable
    public JavaType getReturnType() {
        return EclipseJavaType.create(getBinding().getReturnType());
    }

    @Override
    public boolean isVararg() {
        return getBinding().isVarargs();
    }

    @Override
    public boolean isConstructor() {
        return getBinding().isConstructor();
    }

    @Override
    @NotNull
    public JavaClass getContainingClass() {
        return new EclipseJavaClass(getBinding().getDeclaringClass());
    }
}
