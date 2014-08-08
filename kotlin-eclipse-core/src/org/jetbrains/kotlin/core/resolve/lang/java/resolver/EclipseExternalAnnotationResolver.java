package org.jetbrains.kotlin.core.resolve.lang.java.resolver;

import java.util.Collection;
import java.util.Collections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.resolver.ExternalAnnotationResolver;
import org.jetbrains.jet.lang.resolve.java.structure.JavaAnnotation;
import org.jetbrains.jet.lang.resolve.java.structure.JavaAnnotationOwner;
import org.jetbrains.jet.lang.resolve.name.FqName;

public class EclipseExternalAnnotationResolver implements ExternalAnnotationResolver {

    @Override
    @Nullable
    public JavaAnnotation findExternalAnnotation(@NotNull JavaAnnotationOwner owner, @NotNull FqName fqName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    @NotNull
    public Collection<JavaAnnotation> findExternalAnnotations(@NotNull JavaAnnotationOwner owner) {
        // TODO Auto-generated method stub
        return Collections.emptyList();
    }

}
