package org.jetbrains.kotlin.core.resolve.lang.java.structure;

import java.util.List;

import org.eclipse.jdt.core.IJavaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.structure.JavaAnnotationArgument;
import org.jetbrains.jet.lang.resolve.java.structure.JavaArrayAnnotationArgument;
import org.jetbrains.jet.lang.resolve.name.Name;

import com.google.common.collect.Lists;

public class EclipseJavaArrayAnnotationArgument implements JavaArrayAnnotationArgument {

    private final Object[] arguments;
    private final Name name;
    private final IJavaProject javaProject;
    
    protected EclipseJavaArrayAnnotationArgument(@NotNull Object[] arguments, @NotNull Name name,
            @NotNull IJavaProject javaProject) {
        this.arguments = arguments;
        this.name = name;
        this.javaProject = javaProject;
    }

    @Override
    @NotNull
    public List<JavaAnnotationArgument> getElements() {
        List<JavaAnnotationArgument> annotationArguments = Lists.newArrayList();
        for (Object argument : arguments) {
            annotationArguments.add(EclipseJavaAnnotationArgument.create(argument, name, javaProject));
        }
        
        return annotationArguments;
    }

    @Override
    @Nullable
    public Name getName() {
        return name;
    }
}
