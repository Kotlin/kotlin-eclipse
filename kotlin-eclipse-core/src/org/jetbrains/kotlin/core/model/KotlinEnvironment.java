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
package org.jetbrains.kotlin.core.model;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.asJava.KotlinLightClassForPackage;
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport;
import org.jetbrains.kotlin.cli.jvm.compiler.ClassPath;
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport;
import org.jetbrains.kotlin.cli.jvm.compiler.CoreExternalAnnotationsManager;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension;
import org.jetbrains.kotlin.core.Activator;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.resolve.lang.kotlin.EclipseVirtualFileFinder;
import org.jetbrains.kotlin.core.utils.ProjectUtils;
import org.jetbrains.kotlin.extensions.ExternalDeclarationsProvider;
import org.jetbrains.kotlin.idea.JetFileType;
import org.jetbrains.kotlin.load.kotlin.KotlinBinaryClassCache;
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory;
import org.jetbrains.kotlin.parsing.JetParserDefinition;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.resolve.CodeAnalyzerInitializer;
import org.jetbrains.kotlin.utils.PathUtil;

import com.intellij.codeInsight.ContainerProvider;
import com.intellij.codeInsight.ExternalAnnotationsManager;
import com.intellij.core.CoreApplicationEnvironment;
import com.intellij.core.CoreJavaFileManager;
import com.intellij.core.JavaCoreApplicationEnvironment;
import com.intellij.core.JavaCoreProjectEnvironment;
import com.intellij.mock.MockProject;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.extensions.ExtensionsArea;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElementFinder;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.compiled.ClassFileDecompilers;
import com.intellij.psi.impl.PsiTreeChangePreprocessor;
import com.intellij.psi.impl.compiled.ClsCustomNavigationPolicy;
import com.intellij.psi.impl.file.impl.JavaFileManager;

@SuppressWarnings("deprecation")
public class KotlinEnvironment {
    
    public final static String KT_JDK_ANNOTATIONS_PATH = ProjectUtils.buildLibPath("kotlin-jdk-annotations");
    public final static String KOTLIN_COMPILER_PATH = ProjectUtils.buildLibPath("kotlin-compiler");
    
    private static final Map<IJavaProject, KotlinEnvironment> cachedEnvironment = new HashMap<>();
    private static final Object environmentLock = new Object();
    
    private final JavaCoreApplicationEnvironment applicationEnvironment;
    private final JavaCoreProjectEnvironment projectEnvironment;
    private final MockProject project;
    private final IJavaProject javaProject;
    
    private final ClassPath classPath = new ClassPath();
    
    private KotlinEnvironment(@NotNull IJavaProject javaProject, @NotNull Disposable disposable) {
        this.javaProject = javaProject;
        
        applicationEnvironment = createJavaCoreApplicationEnvironment(disposable);
        
        projectEnvironment = new JavaCoreProjectEnvironment(disposable, applicationEnvironment) {
            @Override
            protected void preregisterServices() {
                registerProjectExtensionPoints(Extensions.getArea(getProject()));
            }
        };
        
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
        project.registerService(KotlinAnalysisProjectCache.class, new KotlinAnalysisProjectCache());
        
        VirtualFile ktJDKAnnotations = PathUtil.jarFileOrDirectoryToVirtualFile(new File(KT_JDK_ANNOTATIONS_PATH));
        annotationsManager.addExternalAnnotationsRoot(ktJDKAnnotations);

        configureClasspath();
        
        project.registerService(VirtualFileFinderFactory.class, new EclipseVirtualFileFinder(classPath));
        
        ExternalDeclarationsProvider.OBJECT$.registerExtensionPoint(project);
        ExpressionCodegenExtension.OBJECT$.registerExtensionPoint(project);
        
        for (String config : EnvironmentConfigFiles.JVM_CONFIG_FILES) {
            registerApplicationExtensionPointsAndExtensionsFrom(config);
        }
        
        cachedEnvironment.put(javaProject, this);
    }
    
    private static void registerProjectExtensionPoints(ExtensionsArea area) {
        CoreApplicationEnvironment.registerExtensionPoint(area, PsiTreeChangePreprocessor.EP_NAME, PsiTreeChangePreprocessor.class);
        CoreApplicationEnvironment.registerExtensionPoint(area, PsiElementFinder.EP_NAME, PsiElementFinder.class);
    }
    
    private static void registerApplicationExtensionPointsAndExtensionsFrom(String configFilePath) {
        File pluginRoot = new File(KOTLIN_COMPILER_PATH);
        CoreApplicationEnvironment.registerExtensionPointAndExtensions(pluginRoot, configFilePath, Extensions.getRootArea());
    }
    
    @NotNull
    public static KotlinEnvironment getEnvironment(IJavaProject javaProject) {
        synchronized (environmentLock) {
            if (!cachedEnvironment.containsKey(javaProject)) {
                cachedEnvironment.put(javaProject, new KotlinEnvironment(javaProject, Disposer.newDisposable()));
            }
            
            return cachedEnvironment.get(javaProject);
        }
    }
    
    public static void updateKotlinEnvironment(IJavaProject javaProject) {
        synchronized (environmentLock) {
            if (cachedEnvironment.containsKey(javaProject)) {
                KotlinEnvironment environment = cachedEnvironment.get(javaProject);
                Disposer.dispose(environment.getJavaApplicationEnvironment().getParentDisposable());
            }
            cachedEnvironment.put(javaProject, new KotlinEnvironment(javaProject, Disposer.newDisposable()));
        }
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
    
    private void configureClasspath() {
        try {
            for (File file : ProjectUtils.collectClasspathWithDependencies(javaProject)) {
                addToClasspath(file);
            }
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
    }
    
    private JavaCoreApplicationEnvironment createJavaCoreApplicationEnvironment(@NotNull Disposable disposable) {
        Extensions.cleanRootArea(disposable);
        registerAppExtensionPoints();
        JavaCoreApplicationEnvironment javaApplicationEnvironment = new JavaCoreApplicationEnvironment(disposable);
        
        // ability to get text from annotations xml files
        javaApplicationEnvironment.registerFileType(PlainTextFileType.INSTANCE, "xml");
        
        javaApplicationEnvironment.registerFileType(JetFileType.INSTANCE, "kt");
        javaApplicationEnvironment.registerFileType(JetFileType.INSTANCE, "jet");
        javaApplicationEnvironment.registerFileType(JetFileType.INSTANCE, "ktm");
        
        javaApplicationEnvironment.registerParserDefinition(new JetParserDefinition());
        
        javaApplicationEnvironment.getApplication().registerService(KotlinBinaryClassCache.class,
                new KotlinBinaryClassCache());
        
        return javaApplicationEnvironment;
    }
    
    private static void registerAppExtensionPoints() {
        CoreApplicationEnvironment.registerExtensionPoint(Extensions.getRootArea(), ContainerProvider.EP_NAME,
                ContainerProvider.class);
        CoreApplicationEnvironment.registerExtensionPoint(Extensions.getRootArea(), ClsCustomNavigationPolicy.EP_NAME,
                ClsCustomNavigationPolicy.class);
        CoreApplicationEnvironment.registerExtensionPoint(Extensions.getRootArea(), ClassFileDecompilers.EP_NAME,
                ClassFileDecompilers.Decompiler.class);
    }
    
    @NotNull
    public Project getProject() {
        return project;
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