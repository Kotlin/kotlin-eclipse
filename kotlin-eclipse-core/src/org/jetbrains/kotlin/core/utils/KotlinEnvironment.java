package org.jetbrains.kotlin.core.utils;

import java.io.File;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.CompilerModeProvider;
import org.jetbrains.jet.OperationModeProvider;
import org.jetbrains.jet.cli.jvm.compiler.CoreExternalAnnotationsManager;
import org.jetbrains.jet.lang.parsing.JetParserDefinition;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.utils.PathUtil;
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
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

public class KotlinEnvironment {
    
    
    private static final Disposable DISPOSABLE = new Disposable() {
        
        @Override
        public void dispose() {
        }
    };
    
    private static final ConcurrentMap<IJavaProject, KotlinEnvironment> cachedEnvironment = new ConcurrentHashMap<>();
    
    private final JavaCoreApplicationEnvironment applicationEnvironment;
    private final JavaCoreProjectEnvironment projectEnvironment;
    private final MockProject project;
    private final IJavaProject javaProject;
    
    private KotlinEnvironment(@NotNull IJavaProject javaProject) {
        this.javaProject = javaProject;
        
        applicationEnvironment = createJavaCoreApplicationEnvironment();
        
        projectEnvironment = new JavaCoreProjectEnvironment(DISPOSABLE, applicationEnvironment);
        
        project = projectEnvironment.getProject();
        
        CoreExternalAnnotationsManager annotationsManager = new CoreExternalAnnotationsManager(project.getComponent(PsiManager.class));
        project.registerService(ExternalAnnotationsManager.class, annotationsManager);
        
        VirtualFile ktJDKAnnotations = PathUtil.jarFileOrDirectoryToVirtualFile(new File(LaunchConfigurationDelegate.KT_JDK_ANNOTATIONS));
        annotationsManager.addExternalAnnotationsRoot(ktJDKAnnotations);
        
        addJreClasspath();
        addKotlinRuntime();
        addSourcesToClasspath();
        addLibsToClasspath();
        
        cachedEnvironment.put(javaProject, this);   
    }
    
    @NotNull
    public static KotlinEnvironment getEnvironment(IJavaProject javaProject) {
        if (!cachedEnvironment.containsKey(javaProject)) {
            cachedEnvironment.put(javaProject, new KotlinEnvironment(javaProject));
        }
        
        return cachedEnvironment.get(javaProject);
    }
    
    public static void updateKotlinEnvironment(IJavaProject javaProject) {
        cachedEnvironment.put(javaProject, new KotlinEnvironment(javaProject));
    }
    
    @Nullable
    public JetFile getJetFile(@NotNull IFile file) {
        return getJetFile(new File(file.getRawLocation().toOSString()));
    }
    
    @Nullable
    public JetFile getJetFile(@NotNull File file) {
        String path = file.getAbsolutePath();
        VirtualFile virtualFile = applicationEnvironment.getLocalFileSystem().findFileByPath(path);
        
        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        if (psiFile != null && psiFile instanceof JetFile) {
            return (JetFile) psiFile;
        }

        return null;
    }

    private JavaCoreApplicationEnvironment createJavaCoreApplicationEnvironment() {
        JavaCoreApplicationEnvironment javaApplicationEnvironment = new JavaCoreApplicationEnvironment(DISPOSABLE);
        
        javaApplicationEnvironment.registerFileType(JetFileType.INSTANCE, "kt");
        javaApplicationEnvironment.registerFileType(JetFileType.INSTANCE, "jet");
        javaApplicationEnvironment.registerParserDefinition(new JetParserDefinition());

        javaApplicationEnvironment.getApplication().registerService(OperationModeProvider.class, new CompilerModeProvider());
        
        return javaApplicationEnvironment;
    }
    
    private void addLibsToClasspath() {
        try {
            List<File> libDirectories = ProjectUtils.getLibDirectories(javaProject);
            for (File libDirectory : libDirectories) {
                addToClasspath(libDirectory);
            }
        } catch (JavaModelException e) {
            KotlinLogger.logAndThrow(e);
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
    }
    
    private void addSourcesToClasspath() {
        try {
            List<File> srcDirectories = ProjectUtils.getSrcDirectories(javaProject);
            for (File srcDirectory : srcDirectories) {
                addToClasspath(srcDirectory);
            }
        } catch (JavaModelException e) {
            KotlinLogger.logAndThrow(e);
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
    }
    
    private void addKotlinRuntime() {
        try {
            addToClasspath(new File(LaunchConfigurationDelegate.KT_RUNTIME_PATH));
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
    }

    private void addJreClasspath() {
        try {
            IRuntimeClasspathEntry computeJREEntry = JavaRuntime.computeJREEntry(javaProject);
            if (computeJREEntry == null) {
                return;
            }
            
            IRuntimeClasspathEntry[] jreEntries = JavaRuntime.resolveRuntimeClasspathEntry(computeJREEntry,
                    javaProject);

            if (jreEntries.length != 0) {
                for (IRuntimeClasspathEntry jreEntry : jreEntries) {
                    addToClasspath(jreEntry.getClasspathEntry().getPath().toFile());
                }
                
                return;
            }
        } catch (JavaModelException e) {
            KotlinLogger.logAndThrow(e);
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
    }
    
    @NotNull
    public Project getProject() {
        return project;
    }
    
    public JavaCoreProjectEnvironment getProjectEnvironment() {
        return projectEnvironment;
    }
    
    @NotNull
    public JavaCoreApplicationEnvironment getJavaApplicationEnvironment() {
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