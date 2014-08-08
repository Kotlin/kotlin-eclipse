package org.jetbrains.kotlin.core.resolve.lang.java.structure;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass;
import org.jetbrains.jet.lang.resolve.java.structure.JavaElement;
import org.jetbrains.jet.lang.resolve.java.structure.JavaPackage;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.resolve.lang.java.EclipseJavaClassFinder;

import com.google.common.collect.Lists;

public class EclipseJavaPackage implements JavaElement, JavaPackage {
    
    private final List<IPackageFragment> packages = Lists.newArrayList();

    public EclipseJavaPackage(List<IPackageFragment> packages) {
        this.packages.addAll(packages);
    }
    
    public EclipseJavaPackage(IPackageFragment pckg) {
        this(Collections.singletonList(pckg));
    }

    @Override
    @NotNull
    public Collection<JavaClass> getClasses() {
        List<JavaClass> javaClasses = Lists.newArrayList();
        for (IPackageFragment pckg : packages) {
            javaClasses.addAll(getClassesInPackage(pckg));
        }
        
        return javaClasses;
    }
    
    @Override
    @NotNull
    public Collection<JavaPackage> getSubPackages() {
        List<JavaPackage> javaPackages = Lists.newArrayList();
        for (IPackageFragment packageFragment : packages) {
            javaPackages.addAll(getSubPackagesFor(packageFragment));
        }
        
        return javaPackages;
    }

    private List<JavaPackage> getSubPackagesFor(IPackageFragment packageFragment) {
        try {
            IJavaElement parent = packageFragment.getParent();
            if (parent instanceof IPackageFragmentRoot) {
                IPackageFragmentRoot packageFragmentRoot = (IPackageFragmentRoot) parent;
                List<JavaPackage> subPackages = Lists.newArrayList();
                for (IJavaElement child : packageFragmentRoot.getChildren()) {
                    if (!(child instanceof IPackageFragment)) continue;
                    
                    IPackageFragment subPackageFragment = (IPackageFragment) child;
                    if (isSubPackageFor(packageFragment, subPackageFragment)) {
                        subPackages.add(new EclipseJavaPackage(subPackageFragment));
                    }
                }
                
                return subPackages;
            }
        } catch (JavaModelException e) {
            KotlinLogger.logAndThrow(e);
        }
        
        return Collections.emptyList();
    }
    
    private boolean isSubPackageFor(IPackageFragment rootPackage, IPackageFragment subPackage) {
        FqName rootFqName = new FqName(rootPackage.getElementName());
        FqName subFqName = new FqName(subPackage.getElementName());
        
        if (!subFqName.isRoot()) {
            return subFqName.parent().equals(rootFqName);
        }
        
        return false;
    }
    
    @Override
    @NotNull
    public FqName getFqName() {
        return new FqName(packages.get(0).getElementName()); // They all should have same names
    }
    
    private List<JavaClass> getClassesInPackage(IPackageFragment javaPackage) {
        try {
            List<JavaClass> javaClasses = Lists.newArrayList();
            for (IClassFile classFile : javaPackage.getClassFiles()) {
                IType type = classFile.getType();
                if (isOuterClass(classFile)) {
                    ITypeBinding typeBinding = EclipseJavaClassFinder.createTypeBinding(type);
                    if (typeBinding != null) {
                        javaClasses.add(new EclipseJavaClass(typeBinding));
                    }
                }
            }
            
            for (ICompilationUnit cu : javaPackage.getCompilationUnits()) {
                for (IType javaClass : cu.getAllTypes()) {
                    if (Name.isValidIdentifier(javaClass.getElementName())) {
                        ITypeBinding typeBinding = EclipseJavaClassFinder.createTypeBinding(javaClass);
                        if (typeBinding != null) {
                            javaClasses.add(new EclipseJavaClass(typeBinding));
                        }
                    }
                }
            }
            
            return javaClasses;
        } catch (JavaModelException e) {
            KotlinLogger.logAndThrow(e);
            throw new IllegalStateException(e);
        }
    }
    
    private boolean isOuterClass(IClassFile classFile) {
        return !classFile.getElementName().contains("$");
    }
}
