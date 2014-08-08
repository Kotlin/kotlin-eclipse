package org.jetbrains.kotlin.core.resolve.lang.java.structure;

import org.eclipse.jdt.core.dom.IVariableBinding;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.structure.JavaElement;
import org.jetbrains.jet.lang.resolve.java.structure.JavaReferenceAnnotationArgument;

public class EclipseJavaReferenceAnnotationArgument extends EclipseJavaAnnotationArgument<IVariableBinding>
        implements JavaReferenceAnnotationArgument {

    protected EclipseJavaReferenceAnnotationArgument(IVariableBinding javaElement) {
        super(javaElement);
    }

    @Override
    @Nullable
    public JavaElement resolve() {
        return new EclipseJavaField(getBinding());
    }
}
