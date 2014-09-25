package org.jetbrains.kotlin.core.resolve.lang.java.structure;

import static org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaElementFactory.typeParameters;

import java.util.List;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass;
import org.jetbrains.jet.lang.resolve.java.structure.JavaConstructor;
import org.jetbrains.jet.lang.resolve.java.structure.JavaTypeParameter;
import org.jetbrains.jet.lang.resolve.java.structure.JavaValueParameter;

public class EclipseJavaConstructor extends EclipseJavaMember<IMethodBinding> implements JavaConstructor {
    
    public EclipseJavaConstructor(@NotNull IMethodBinding methodBinding) {
        super(methodBinding);
        assert methodBinding.isConstructor() :
            "Method binding which is not a constructor should not be wrapped in EclipseJavaConstructor: " + methodBinding.getName();
    }
    
    @Override
    @NotNull
    public JavaClass getContainingClass() {
        return new EclipseJavaClass(getBinding().getDeclaringClass());
    }
    
    @Override
    @NotNull
    public List<JavaTypeParameter> getTypeParameters() {
        return typeParameters(getBinding().getTypeParameters());
    }
    
    @Override
    @NotNull
    public List<JavaValueParameter> getValueParameters() {
        return EclipseJavaElementUtil.getValueParameters(getBinding());
    }
    
}
