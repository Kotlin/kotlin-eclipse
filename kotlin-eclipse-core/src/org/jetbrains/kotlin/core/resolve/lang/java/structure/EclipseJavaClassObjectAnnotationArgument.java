package org.jetbrains.kotlin.core.resolve.lang.java.structure;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClassObjectAnnotationArgument;
import org.jetbrains.jet.lang.resolve.java.structure.JavaType;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.kotlin.core.resolve.lang.java.EclipseJavaClassFinder;

public class EclipseJavaClassObjectAnnotationArgument implements JavaClassObjectAnnotationArgument {

    private final Class<?> javaClass;
    private final IJavaProject javaProject;
    private final Name name;
    
    protected EclipseJavaClassObjectAnnotationArgument(Class<?> javaClass, @NotNull Name name, @NotNull IJavaProject javaProject) {
        this.javaClass = javaClass;
        this.name = name;
        this.javaProject = javaProject;
    }

    @Override
    @NotNull
    public JavaType getReferencedType() {
        ITypeBinding typeBinding = EclipseJavaClassFinder.findType(new FqName(javaClass.getCanonicalName()), javaProject);
        assert typeBinding != null;
        return EclipseJavaType.create(typeBinding);
    }

    @Override
    @Nullable
    public Name getName() {
        return name;
    }
}
