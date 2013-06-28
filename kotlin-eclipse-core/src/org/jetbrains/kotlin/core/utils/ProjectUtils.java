package org.jetbrains.kotlin.core.utils;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.core.resources.IFile;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.plugin.JetMainDetector;

public class ProjectUtils {
    
    public static IFile getMainClass(Collection<IFile> files) {
        KotlinEnvironment kotlinEnvironment = new KotlinEnvironment();
        
        for (IFile file : files) {
            if (JetMainDetector.hasMain(kotlinEnvironment.getJetFile(file).getDeclarations())) {
                return file;
            }
        }
                
        return null;
    }
    
    public static boolean hasMain(IFile file) {
        return getMainClass(Arrays.asList(file)) != null;
    }
    
    public static String getPackageByFile(IFile file) {
        return new KotlinEnvironment().getJetFile(file).getPackageName();
    }
    
    public static FqName createPackageClassName(IFile file) {
        return PackageClassUtils.getPackageClassFqName(new FqName(getPackageByFile(file)));
    }
}