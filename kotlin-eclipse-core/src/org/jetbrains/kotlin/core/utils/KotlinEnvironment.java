/*******************************************************************************
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *******************************************************************************/
package org.jetbrains.kotlin.core.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
import org.jetbrains.jet.asJava.KotlinLightClassForPackage;
import org.jetbrains.jet.asJava.LightClassGenerationSupport;
import org.jetbrains.jet.cli.jvm.compiler.ClassPath;
import org.jetbrains.jet.cli.jvm.compiler.CliLightClassGenerationSupport;
import org.jetbrains.jet.cli.jvm.compiler.CoreExternalAnnotationsManager;
import org.jetbrains.jet.lang.parsing.JetParserDefinition;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.CodeAnalyzerInitializer;
import org.jetbrains.jet.lang.resolve.diagnostics.DiagnosticsWithSuppression;
import org.jetbrains.jet.lang.resolve.kotlin.KotlinBinaryClassCache;
import org.jetbrains.jet.lang.resolve.kotlin.VirtualFileFinderFactory;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.utils.PathUtil;
import org.jetbrains.kotlin.core.Activator;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.resolve.lang.kotlin.EclipseVirtualFileFinder;

import com.intellij.codeInsight.ExternalAnnotationsManager;
import com.intellij.core.CoreApplicationEnvironment;
import com.intellij.core.CoreJavaFileManager;
import com.intellij.core.JavaCoreApplicationEnvironment;
import com.intellij.core.JavaCoreProjectEnvironment;
import com.intellij.mock.MockProject;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.compiled.ClassFileDecompilers;
import com.intellij.psi.impl.compiled.ClsCustomNavigationPolicy;
import com.intellij.psi.impl.file.impl.JavaFileManager;

public class KotlinEnvironment {
    
    public final static String KT_JDK_ANNOTATIONS_PATH = ProjectUtils.buildLibPath("kotlin-jdk-annotations");
    
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
    
    private final ClassPath classPath = new ClassPath();
    
    private KotlinEnvironment(@NotNull IJavaProject javaProject) {
        this.javaProject = javaProject;
        
        applicationEnvironment = createJavaCoreApplicationEnvironment();
        
        projectEnvironment = new JavaCoreProjectEnvironment(DISPOSABLE, applicationEnvironment);
        
        project = projectEnvironment.getProject();
        
        CoreExternalAnnotationsManager annotationsManager = new CoreExternalAnnotationsManager(
                project.getComponent(PsiManager.class));
        project.registerService(ExternalAnnotationsManager.class, annotationsManager);
        project.registerService(CoreJavaFileManager.class,
                (CoreJavaFileManager) ServiceManager.getService(project, JavaFileManager.class));
        
        CliLightClassGenerationSupport cliLightClassGenerationSupport = new CliLightClassGenerationSupport(project);
        project.registerService(LightClassGenerationSupport.class, cliLightClassGenerationSupport);
        project.registerService(CliLightClassGenerationSupport.class, cliLightClassGenerationSupport);
        project.registerService(KotlinLightClassForPackage.FileStubCache.class, new KotlinLightClassForPackage.FileStubCache(project));
        project.registerService(CodeAnalyzerInitializer.class, cliLightClassGenerationSupport);
        
        VirtualFile ktJDKAnnotations = PathUtil.jarFileOrDirectoryToVirtualFile(new File(KT_JDK_ANNOTATIONS_PATH));
        annotationsManager.addExternalAnnotationsRoot(ktJDKAnnotations);
        
        addJreClasspath();
        addSourcesToClasspath();
        addLibsToClasspath();
        
        project.registerService(VirtualFileFinderFactory.class, new EclipseVirtualFileFinder(classPath));
        
        CoreApplicationEnvironment.registerExtensionPoint(Extensions.getRootArea(), ClsCustomNavigationPolicy.EP_NAME,
                ClsCustomNavigationPolicy.class);
        CoreApplicationEnvironment.registerExtensionPoint(Extensions.getRootArea(), ClassFileDecompilers.EP_NAME,
                ClassFileDecompilers.Decompiler.class);
        
        CoreApplicationEnvironment.registerApplicationExtensionPoint(DiagnosticsWithSuppression.SuppressStringProvider.EP_NAME,
                DiagnosticsWithSuppression.SuppressStringProvider.class);

        CoreApplicationEnvironment.registerApplicationExtensionPoint(DiagnosticsWithSuppression.DiagnosticSuppressor.EP_NAME,
                DiagnosticsWithSuppression.DiagnosticSuppressor.class);
        
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
    
    @Nullable
    public JetFile parseTopLevelDeclaration(@NotNull String text) {
        try {
            File tempFile;
            tempFile = File.createTempFile("temp", "." + JetFileType.INSTANCE.getDefaultExtension());
            BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile));
            bw.write(text);
            bw.close();
            
            return getJetFile(tempFile);
        } catch (IOException e) {
            KotlinLogger.logError(e);
            throw new IllegalStateException(e);
        }
    }
    
    private JavaCoreApplicationEnvironment createJavaCoreApplicationEnvironment() {
        JavaCoreApplicationEnvironment javaApplicationEnvironment = new JavaCoreApplicationEnvironment(DISPOSABLE);
        
        // ability to get text from annotations xml files
        javaApplicationEnvironment.registerFileType(PlainTextFileType.INSTANCE, "xml");
        
        javaApplicationEnvironment.registerFileType(JetFileType.INSTANCE, "kt");
        javaApplicationEnvironment.registerFileType(JetFileType.INSTANCE, "jet");
        javaApplicationEnvironment.registerFileType(JetFileType.INSTANCE, "ktm");
        
        javaApplicationEnvironment.registerParserDefinition(new JetParserDefinition());
        
        javaApplicationEnvironment.getApplication().registerService(OperationModeProvider.class,
                new CompilerModeProvider());
        javaApplicationEnvironment.getApplication().registerService(KotlinBinaryClassCache.class,
                new KotlinBinaryClassCache());
        
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
            for (File srcDirectory : ProjectUtils.getSrcDirectories(javaProject)) {
                addToClasspath(srcDirectory);
            }
            
            for (File srcDirectory : ProjectUtils.collectDependenciesClasspath(javaProject)) {
                addToClasspath(srcDirectory);
            }
        } catch (JavaModelException e) {
            KotlinLogger.logAndThrow(e);
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
            
            IRuntimeClasspathEntry[] jreEntries = JavaRuntime.resolveRuntimeClasspathEntry(computeJREEntry, javaProject);
            
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
            classPath.add(jarFile);
        } else {
            VirtualFile root = applicationEnvironment.getLocalFileSystem().findFileByPath(path.getAbsolutePath());
            if (root == null) {
                throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Can't find jar: " + path));
            }
            projectEnvironment.addSourcesToClasspath(root);
            classPath.add(root);
        }
    }
}