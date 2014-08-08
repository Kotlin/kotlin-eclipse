package org.jetbrains.kotlin.core.resolve.lang.java.structure;

import java.lang.reflect.Modifier;
import java.util.Collection;

import org.eclipse.jdt.core.dom.IBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.Visibility;
import org.jetbrains.jet.lang.resolve.java.structure.JavaAnnotation;
import org.jetbrains.jet.lang.resolve.java.structure.JavaMember;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

public abstract class EclipseJavaMember<T extends IBinding> extends EclipseJavaElement<T> implements JavaMember {

    protected EclipseJavaMember(@NotNull T javaElement) {
        super(javaElement);
    }
    
    @Override
    @NotNull
    public Collection<JavaAnnotation> getAnnotations() {
        return EclipseJavaElementUtil.getAnnotations(getBinding());
    }

    @Override
    @Nullable
    public JavaAnnotation findAnnotation(@NotNull FqName fqName) {
        return EclipseJavaElementUtil.findAnnotation(getBinding(), fqName);
    }

    @Override
    public boolean isAbstract() {
        return Modifier.isAbstract(getBinding().getModifiers());
    }

    @Override
    public boolean isStatic() {
        return Modifier.isStatic(getBinding().getModifiers());
    }

    @Override
    public boolean isFinal() {
        return Modifier.isFinal(getBinding().getModifiers());
    }

    @Override
    @NotNull
    public Visibility getVisibility() {
        return EclipseJavaElementUtil.getVisibility(getBinding());
    }

    @Override
    @NotNull
    public Name getName() {
        return Name.guess(getBinding().getName());
    }
}
