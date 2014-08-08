package org.jetbrains.kotlin.core.resolve.lang.java.structure;

import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.structure.JavaAnnotation;
import org.jetbrains.jet.lang.resolve.java.structure.JavaAnnotationArgument;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import com.google.common.collect.Lists;

public class EclipseJavaAnnotation extends EclipseJavaElement<IAnnotationBinding> implements JavaAnnotation {

    protected EclipseJavaAnnotation(IAnnotationBinding javaAnnotation) {
        super(javaAnnotation);
    }

    @Override
    @Nullable
    public JavaAnnotationArgument findArgument(@NotNull Name name) {
        for (IMemberValuePairBinding member : getBinding().getDeclaredMemberValuePairs()) {
            if (name.equals(member.getName())) {
                return EclipseJavaAnnotationArgument.create(member.getValue(), name, getJavaProject());
            }
        }
        
        return null;
    }

    @Override
    @NotNull
    public Collection<JavaAnnotationArgument> getArguments() {
        List<JavaAnnotationArgument> arguments = Lists.newArrayList();
        for (IMemberValuePairBinding memberValuePair : getBinding().getDeclaredMemberValuePairs()) {
            arguments.add(EclipseJavaAnnotationArgument.create(
                    memberValuePair.getValue(), 
                    Name.identifier(memberValuePair.getName()), 
                    getJavaProject()));
        }
        
        return arguments;
    }

    @Override
    @Nullable
    public FqName getFqName() {
        return new FqName(getBinding().getName());
    }

    @Override
    @Nullable
    public JavaClass resolve() {
        return new EclipseJavaClass(getBinding().getAnnotationType());
    }

}
