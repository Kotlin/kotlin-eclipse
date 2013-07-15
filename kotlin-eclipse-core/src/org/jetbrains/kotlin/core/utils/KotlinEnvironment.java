package org.jetbrains.kotlin.core.utils;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.jetbrains.jet.CompilerModeProvider;
import org.jetbrains.jet.OperationModeProvider;
import org.jetbrains.jet.cli.jvm.compiler.CoreExternalAnnotationsManager;
import org.jetbrains.jet.lang.parsing.JetParserDefinition;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.kotlin.core.Activator;
import org.jetbrains.kotlin.core.launch.LaunchConfigurationDelegate;
import org.jetbrains.kotlin.core.log.KotlinLogger;

import com.intellij.codeInsight.ExternalAnnotationsManager;
import com.intellij.core.JavaCoreApplicationEnvironment;
import com.intellij.core.JavaCoreProjectEnvironment;
import com.intellij.mock.MockProject;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;

public class KotlinEnvironment {
    
    private final JavaCoreApplicationEnvironment applicationEnvironment;
    private final JavaCoreProjectEnvironment projectEnvironment;
    private final MockProject project;
    
    private final static Disposable DISPOSABLE = new Disposable() {
        
        @Override
        public void dispose() {
        }
    };
    
    public KotlinEnvironment() {
        applicationEnvironment = new JavaCoreApplicationEnvironment(DISPOSABLE);
        
        applicationEnvironment.registerFileType(JetFileType.INSTANCE, "kt");
        applicationEnvironment.registerFileType(JetFileType.INSTANCE, "jet");
        applicationEnvironment.registerParserDefinition(new JetParserDefinition());

        applicationEnvironment.getApplication().registerService(OperationModeProvider.class, new CompilerModeProvider());
        
        projectEnvironment = new JavaCoreProjectEnvironment(DISPOSABLE, applicationEnvironment);
        
        project = projectEnvironment.getProject();
        
        CoreExternalAnnotationsManager annotationsManager = new CoreExternalAnnotationsManager(project.getComponent(PsiManager.class));
        project.registerService(ExternalAnnotationsManager.class, annotationsManager);
        
        addJreClasspath();
        addKotlinRuntime();
        addKotlinAnnotations();
    }
    
    private void addKotlinRuntime() {
        try {
            addToClasspath(new File(LaunchConfigurationDelegate.KT_RUNTIME_PATH));
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
    }
    
    private void addKotlinAnnotations() {
        try {
            addToClasspath(new File(LaunchConfigurationDelegate.KT_JDK_ANNOTATIONS));
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
    }

    private void addJreClasspath() {
        try {
            JavaModelManager modelManager = JavaModelManager.getJavaModelManager();
            IJavaProject[] javaProjects = modelManager.getJavaModel().getJavaProjects();

            for (IJavaProject javaProject : javaProjects) {
                IRuntimeClasspathEntry computeJREEntry = JavaRuntime.computeJREEntry(javaProject);
                IRuntimeClasspathEntry[] jreEntries = JavaRuntime.resolveRuntimeClasspathEntry(computeJREEntry,
                        javaProject);

                // TODO: Configuring JRE should be specific for each java project. Now the first java project is used.
                if (jreEntries.length != 0) {
                    for (IRuntimeClasspathEntry jreEntry : jreEntries) {
                        addToClasspath(jreEntry.getClasspathEntry().getPath().toFile());
                    }
                    
                    return;
                }
            }
        } catch (JavaModelException e) {
            KotlinLogger.logError(e);
        } catch (CoreException e) {
            KotlinLogger.logError(e);
        }
    }
    
    public JetFile getJetFile(IFile file) {
        VirtualFile fileByPath = applicationEnvironment.getLocalFileSystem().findFileByPath(
                file.getRawLocation().toOSString());
        
        return (JetFile) PsiManager.getInstance(project).findFile(fileByPath);
    }
    
    public Project getProject() {
        return project;
    }
    
    public JavaCoreProjectEnvironment getProjectEnvironment() {
        return projectEnvironment;
    }
    
    public JavaCoreApplicationEnvironment getApplicationEnvironment() {
        return applicationEnvironment;
    }
    
    private void addToClasspath(File path) throws CoreException {
        if (path.isFile()) {
            VirtualFile jarFile = applicationEnvironment.getJarFileSystem().findFileByPath(path + "!/");
            if (jarFile == null) {
                throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Can't find jar: " + path));
            }
            projectEnvironment.addJarToClassPath(path);
        }
        else {
            VirtualFile root = applicationEnvironment.getLocalFileSystem().findFileByPath(path.getAbsolutePath());
            if (root == null) {
                throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Can't find jar: " + path));
            }
            projectEnvironment.addSourcesToClasspath(root);
        }
    }
}