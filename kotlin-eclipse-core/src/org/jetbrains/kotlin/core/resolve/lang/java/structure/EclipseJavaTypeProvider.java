package org.jetbrains.kotlin.core.resolve.lang.java.structure;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.java.structure.JavaType;
import org.jetbrains.jet.lang.resolve.java.structure.JavaTypeProvider;
import org.jetbrains.jet.lang.resolve.java.structure.JavaWildcardType;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.resolve.lang.java.EclipseJavaClassFinder;

import com.intellij.psi.CommonClassNames;

public class EclipseJavaTypeProvider implements JavaTypeProvider {

    private final IJavaProject javaProject;
    
    public EclipseJavaTypeProvider(@NotNull IJavaProject javaProject) {
        this.javaProject = javaProject;
    }
    
    @Override
    @NotNull
    public JavaType createJavaLangObjectType() {
        try {
            IType type = javaProject.findType(CommonClassNames.JAVA_LANG_OBJECT);
            ITypeBinding typeBinding = EclipseJavaClassFinder.createTypeBinding(type);
            assert typeBinding != null : "Type binding for java.lang.Object can not be null";
            
            return EclipseJavaType.create(typeBinding);
        } catch (JavaModelException e) {
            KotlinLogger.logAndThrow(e);
            throw new IllegalStateException(e);
        }
        
    }

    @Override
    @NotNull
    public JavaWildcardType createUpperBoundWildcard(@NotNull JavaType bound) {
        return new EclipseJavaImmediateWildcardType(bound, true, this);
    }

    @Override
    @NotNull
    public JavaWildcardType createLowerBoundWildcard(@NotNull JavaType bound) {
        return new EclipseJavaImmediateWildcardType(bound, false, this);
    }

    @Override
    @NotNull
    public JavaWildcardType createUnboundedWildcard() {
        return new EclipseJavaImmediateWildcardType(null, false, this);
    }

}
