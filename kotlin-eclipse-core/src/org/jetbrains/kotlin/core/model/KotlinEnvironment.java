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

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.asJava.KotlinLightClassForFacade;
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport;
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport;
import org.jetbrains.kotlin.cli.jvm.compiler.CoreExternalAnnotationsManager;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension;
import org.jetbrains.kotlin.core.filesystem.KotlinLightClassManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.resolve.BuiltInsReferenceResolver;
import org.jetbrains.kotlin.core.resolve.KotlinSourceIndex;
import org.jetbrains.kotlin.core.resolve.lang.kotlin.EclipseVirtualFileFinder;
import org.jetbrains.kotlin.core.utils.ProjectUtils;
import org.jetbrains.kotlin.extensions.ExternalDeclarationsProvider;
import org.jetbrains.kotlin.idea.JetFileType;
import org.jetbrains.kotlin.load.kotlin.JvmVirtualFileFinderFactory;
import org.jetbrains.kotlin.load.kotlin.KotlinBinaryClassCache;
import org.jetbrains.kotlin.parsing.JetParserDefinition;
import org.jetbrains.kotlin.resolve.CodeAnalyzerInitializer;
import org.jetbrains.kotlin.utils.PathUtil;

import com.intellij.codeInsight.ContainerProvider;
import com.intellij.codeInsight.ExternalAnnotationsManager;
import com.intellij.codeInsight.NullableNotNullManager;
import com.intellij.codeInsight.runner.JavaMainMethodProvider;
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
import com.intellij.psi.PsiManager;
import com.intellij.psi.augment.PsiAugmentProvider;
import com.intellij.psi.codeStyle.CodeStyleManager;
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
    private final Set<VirtualFile> roots = new LinkedHashSet<>();
    
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

//        For j2k converter
        project.registerService(NullableNotNullManager.class, new KotlinNullableNotNullManager(javaProject)); 
        
        PsiManager psiManager = project.getComponent(PsiManager.class);
        assert (psiManager != null);
        CoreExternalAnnotationsManager annotationsManager = new CoreExternalAnnotationsManager(psiManager);
        project.registerService(ExternalAnnotationsManager.class, annotationsManager);
        project.registerService(CoreJavaFileManager.class,
                (CoreJavaFileManager) ServiceManager.getService(project, JavaFileManager.class));
        
        CliLightClassGenerationSupport cliLightClassGenerationSupport = new CliLightClassGenerationSupport(project);
        project.registerService(LightClassGenerationSupport.class, cliLightClassGenerationSupport);
        project.registerService(CliLightClassGenerationSupport.class, cliLightClassGenerationSupport);
        project.registerService(KotlinLightClassForFacade.PackageFacadeStubCache.class, new KotlinLightClassForFacade.PackageFacadeStubCache(project));
        project.registerService(KotlinLightClassForFacade.FacadeStubCache.class, new KotlinLightClassForFacade.FacadeStubCache(project));
        project.registerService(CodeAnalyzerInitializer.class, cliLightClassGenerationSupport);
        project.registerService(KotlinLightClassManager.class, new KotlinLightClassManager(javaProject));
        project.registerService(CodeStyleManager.class, new DummyCodeStyleManager());
        project.registerService(BuiltInsReferenceResolver.class, new BuiltInsReferenceResolver(project));
        project.registerService(KotlinSourceIndex.class, new KotlinSourceIndex());
        
        VirtualFile ktJDKAnnotations = PathUtil.jarFileOrDirectoryToVirtualFile(new File(KT_JDK_ANNOTATIONS_PATH));
        annotationsManager.addExternalAnnotationsRoot(ktJDKAnnotations);

        configureClasspath();
        
        project.registerService(JvmVirtualFileFinderFactory.class, new EclipseVirtualFileFinder(javaProject));
        
        ExternalDeclarationsProvider.Companion.registerExtensionPoint(project);
        ExpressionCodegenExtension.Companion.registerExtensionPoint(project);
        
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
    public static KotlinEnvironment getEnvironment(@NotNull IJavaProject javaProject) {
        synchronized (environmentLock) {
            if (!cachedEnvironment.containsKey(javaProject)) {
                cachedEnvironment.put(javaProject, new KotlinEnvironment(javaProject, Disposer.newDisposable()));
            }
            
            return cachedEnvironment.get(javaProject);
        }
    }
    
    public static void updateKotlinEnvironment(@NotNull IJavaProject javaProject) {
        synchronized (environmentLock) {
            if (cachedEnvironment.containsKey(javaProject)) {
                KotlinEnvironment environment = cachedEnvironment.get(javaProject);
                Disposer.dispose(environment.getJavaApplicationEnvironment().getParentDisposable());
            }
            cachedEnvironment.put(javaProject, new KotlinEnvironment(javaProject, Disposer.newDisposable()));
        }
    }
    
    private void configureClasspath() {
        try {
            for (File file : ProjectUtils.collectClasspathWithDependenciesForBuild(javaProject)) {
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
        
        // For j2k converter 
        CoreApplicationEnvironment.registerExtensionPoint(Extensions.getRootArea(), PsiAugmentProvider.EP_NAME, PsiAugmentProvider.class);
        CoreApplicationEnvironment.registerExtensionPoint(Extensions.getRootArea(), JavaMainMethodProvider.EP_NAME, JavaMainMethodProvider.class);
    }
    
    @NotNull
    public Project getProject() {
        return project;
    }
    
    @NotNull
    public JavaCoreApplicationEnvironment getJavaApplicationEnvironment() {
        return applicationEnvironment;
    }
    
    @Nullable
    public VirtualFile getVirtualFile(@NotNull IPath location) {
        return applicationEnvironment.getLocalFileSystem().findFileByIoFile(location.toFile());
    }
    
    public VirtualFile getVirtualFileInJar(@NotNull IPath pathToJar, @NotNull String relativePath) {
        return applicationEnvironment.getJarFileSystem().findFileByPath(pathToJar + "!/" + relativePath);
    }
    
    public boolean isJarFile(@NotNull IPath pathToJar) {
        VirtualFile jarFile = applicationEnvironment.getJarFileSystem().findFileByPath(pathToJar + "!/");
        return jarFile != null && jarFile.isValid();
    }
    
    public Set<VirtualFile> getRoots() {
        return Collections.unmodifiableSet(roots);
    }
    
    private void addToClasspath(File path) throws CoreException {
        if (path.isFile()) {
            VirtualFile jarFile = applicationEnvironment.getJarFileSystem().findFileByPath(path + "!/");
            if (jarFile == null) {
                KotlinLogger.logWarning("Can't find jar: " + path);
                return;
            }
            projectEnvironment.addJarToClassPath(path);
            roots.add(jarFile);
        } else {
            VirtualFile root = applicationEnvironment.getLocalFileSystem().findFileByPath(path.getAbsolutePath());
            if (root == null) {
                KotlinLogger.logWarning("Can't find jar: " + path);
                return;
            }
            projectEnvironment.addSourcesToClasspath(root);
            roots.add(root);
        }
    }
}