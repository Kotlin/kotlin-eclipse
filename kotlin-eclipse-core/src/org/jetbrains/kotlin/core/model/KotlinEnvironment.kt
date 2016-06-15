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
package org.jetbrains.kotlin.core.model

import com.intellij.codeInsight.ContainerProvider
import com.intellij.codeInsight.NullableNotNullManager
import com.intellij.codeInsight.runner.JavaMainMethodProvider
import com.intellij.core.CoreApplicationEnvironment
import com.intellij.core.CoreJavaFileManager
import com.intellij.core.JavaCoreApplicationEnvironment
import com.intellij.core.JavaCoreProjectEnvironment
import com.intellij.formatting.KotlinLanguageCodeStyleSettingsProvider
import com.intellij.formatting.KotlinSettingsProvider
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.extensions.ExtensionsArea
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.augment.PsiAugmentProvider
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider
import com.intellij.psi.compiled.ClassFileDecompilers
import com.intellij.psi.impl.PsiTreeChangePreprocessor
import com.intellij.psi.impl.compiled.ClsCustomNavigationPolicy
import com.intellij.psi.impl.file.impl.JavaFileManager
import org.eclipse.core.runtime.IPath
import org.eclipse.jdt.core.IJavaProject
import org.jetbrains.kotlin.asJava.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.cli.common.CliModuleVisibilityManagerImpl
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.core.filesystem.KotlinLightClassManager
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.core.resolve.BuiltInsReferenceResolver
import org.jetbrains.kotlin.core.resolve.KotlinCacheServiceImpl
import org.jetbrains.kotlin.core.resolve.KotlinSourceIndex
import org.jetbrains.kotlin.core.resolve.lang.kotlin.EclipseVirtualFileFinder
import org.jetbrains.kotlin.core.utils.KotlinImportInserterHelper
import org.jetbrains.kotlin.core.utils.ProjectUtils
import org.jetbrains.kotlin.extensions.ExternalDeclarationsProvider
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.load.kotlin.JvmVirtualFileFinderFactory
import org.jetbrains.kotlin.load.kotlin.KotlinBinaryClassCache
import org.jetbrains.kotlin.load.kotlin.ModuleVisibilityManager
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.resolve.CodeAnalyzerInitializer
import org.jetbrains.kotlin.script.KotlinScriptDefinitionProvider
import org.jetbrains.kotlin.script.StandardScriptDefinition
import java.io.File
import java.util.LinkedHashSet
import kotlin.reflect.KClass

<<<<<<< HEAD
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.asJava.KtLightClassForFacade;
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport;
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService;
import org.jetbrains.kotlin.cli.common.CliModuleVisibilityManagerImpl;
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension;
import org.jetbrains.kotlin.core.filesystem.KotlinLightClassManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.resolve.BuiltInsReferenceResolver;
import org.jetbrains.kotlin.core.resolve.KotlinCacheServiceImpl;
import org.jetbrains.kotlin.core.resolve.KotlinSourceIndex;
import org.jetbrains.kotlin.core.resolve.lang.kotlin.EclipseVirtualFileFinder;
import org.jetbrains.kotlin.core.utils.KotlinImportInserterHelper;
import org.jetbrains.kotlin.core.utils.ProjectUtils;
import org.jetbrains.kotlin.extensions.ExternalDeclarationsProvider;
import org.jetbrains.kotlin.idea.KotlinFileType;
import org.jetbrains.kotlin.idea.util.ImportInsertHelper;
import org.jetbrains.kotlin.load.kotlin.JvmVirtualFileFinderFactory;
import org.jetbrains.kotlin.load.kotlin.KotlinBinaryClassCache;
import org.jetbrains.kotlin.load.kotlin.ModuleVisibilityManager;
import org.jetbrains.kotlin.parsing.KotlinParserDefinition;
import org.jetbrains.kotlin.resolve.CodeAnalyzerInitializer;

import com.intellij.codeInsight.ContainerProvider;
import com.intellij.codeInsight.NullableNotNullManager;
import com.intellij.codeInsight.runner.JavaMainMethodProvider;
import com.intellij.core.CoreApplicationEnvironment;
import com.intellij.core.CoreJavaFileManager;
import com.intellij.core.JavaCoreApplicationEnvironment;
import com.intellij.core.JavaCoreProjectEnvironment;
import com.intellij.formatting.KotlinLanguageCodeStyleSettingsProvider;
import com.intellij.formatting.KotlinSettingsProvider;
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
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider;
import com.intellij.psi.compiled.ClassFileDecompilers;
import com.intellij.psi.impl.PsiTreeChangePreprocessor;
import com.intellij.psi.impl.compiled.ClsCustomNavigationPolicy;
import com.intellij.psi.impl.file.impl.JavaFileManager;

import kotlin.jvm.functions.Function1;
=======
val KT_JDK_ANNOTATIONS_PATH = ProjectUtils.buildLibPath("kotlin-jdk-annotations")
val KOTLIN_COMPILER_PATH = ProjectUtils.buildLibPath("kotlin-compiler")
>>>>>>> ca7e3f6... J2K Kotlin Environment: convert and prettify

@SuppressWarnings("deprecation")
class KotlinEnvironment private constructor(val javaProject: IJavaProject, disposable: Disposable) {
    val javaApplicationEnvironment: JavaCoreApplicationEnvironment
    val project: MockProject
    
<<<<<<< HEAD
    private KotlinEnvironment(@NotNull IJavaProject javaProject, @NotNull Disposable disposable) {
        this.javaProject = javaProject;
        
        applicationEnvironment = createJavaCoreApplicationEnvironment(disposable);
=======
    private val projectEnvironment: JavaCoreProjectEnvironment
    private val roots = LinkedHashSet<VirtualFile>()
    
    val configuration = CompilerConfiguration()

    init {
        javaApplicationEnvironment = createJavaCoreApplicationEnvironment(disposable)
>>>>>>> ca7e3f6... J2K Kotlin Environment: convert and prettify
        
        projectEnvironment = object : JavaCoreProjectEnvironment(disposable, javaApplicationEnvironment) {
            override fun preregisterServices() {
                registerProjectExtensionPoints(Extensions.getArea(getProject()))
            }
        }
        
<<<<<<< HEAD
        project.registerService(ModuleVisibilityManager.class, new CliModuleVisibilityManagerImpl());

//        For j2k converter
        project.registerService(NullableNotNullManager.class, new KotlinNullableNotNullManager(javaProject)); 
        
        PsiManager psiManager = project.getComponent(PsiManager.class);
        assert (psiManager != null);
        project.registerService(CoreJavaFileManager.class,
                (CoreJavaFileManager) ServiceManager.getService(project, JavaFileManager.class));
        
        CliLightClassGenerationSupport cliLightClassGenerationSupport = new CliLightClassGenerationSupport(project);
        project.registerService(LightClassGenerationSupport.class, cliLightClassGenerationSupport);
        project.registerService(CliLightClassGenerationSupport.class, cliLightClassGenerationSupport);
        project.registerService(KtLightClassForFacade.FacadeStubCache.class, new KtLightClassForFacade.FacadeStubCache(project));
        project.registerService(CodeAnalyzerInitializer.class, cliLightClassGenerationSupport);
        project.registerService(KotlinLightClassManager.class, new KotlinLightClassManager(javaProject));
        project.registerService(CodeStyleManager.class, new DummyCodeStyleManager());
        project.registerService(BuiltInsReferenceResolver.class, new BuiltInsReferenceResolver(project));
        project.registerService(KotlinSourceIndex.class, new KotlinSourceIndex());
        project.registerService(KotlinCacheService.class, new KotlinCacheServiceImpl());
        project.registerService(ImportInsertHelper.class, new KotlinImportInserterHelper());
        
        configureClasspath();
=======
        project = projectEnvironment.getProject()
        
        with(project) {
            registerService(KotlinScriptDefinitionProvider::class.java, KotlinScriptDefinitionProvider())
            registerService(ModuleVisibilityManager::class.java, CliModuleVisibilityManagerImpl())

            // For j2k converter
            registerService(NullableNotNullManager::class.java, KotlinNullableNotNullManager())

            registerService(CoreJavaFileManager::class.java,
                    ServiceManager.getService(project, JavaFileManager::class.java) as CoreJavaFileManager)

            val cliLightClassGenerationSupport = CliLightClassGenerationSupport(project)
            registerService(LightClassGenerationSupport::class.java, cliLightClassGenerationSupport)
            registerService(CliLightClassGenerationSupport::class.java, cliLightClassGenerationSupport)
            registerService(CodeAnalyzerInitializer::class.java, cliLightClassGenerationSupport)
            
            registerService(KtLightClassForFacade.FacadeStubCache::class.java, KtLightClassForFacade.FacadeStubCache(project))
            registerService(KotlinLightClassManager::class.java, KotlinLightClassManager(javaProject.project))
            registerService(CodeStyleManager::class.java, DummyCodeStyleManager())
            registerService(BuiltInsReferenceResolver::class.java, BuiltInsReferenceResolver(project))
            registerService(KotlinSourceIndex::class.java, KotlinSourceIndex())
            registerService(KotlinCacheService::class.java, KotlinCacheServiceImpl())
            registerService(ImportInsertHelper::class.java, KotlinImportInserterHelper())
            registerService(JvmVirtualFileFinderFactory::class.java, EclipseVirtualFileFinder(javaProject))
        }
        
        configuration.put(CommonConfigurationKeys.MODULE_NAME, project.getName())
>>>>>>> ca7e3f6... J2K Kotlin Environment: convert and prettify
        
        KotlinScriptDefinitionProvider.getInstance(project).addScriptDefinition(StandardScriptDefinition)
        
        configureClasspath()
        
        ExternalDeclarationsProvider.Companion.registerExtensionPoint(project)
        ExpressionCodegenExtension.Companion.registerExtensionPoint(project)
        for (config in EnvironmentConfigFiles.JVM_CONFIG_FILES) {
            registerApplicationExtensionPointsAndExtensionsFrom(config)
        }
        
<<<<<<< HEAD
        cachedEnvironment.putEnvironment(javaProject, this);
    }
    
    private static void registerProjectExtensionPoints(ExtensionsArea area) {
        CoreApplicationEnvironment.registerExtensionPoint(area, PsiTreeChangePreprocessor.EP_NAME, PsiTreeChangePreprocessor.class);
        CoreApplicationEnvironment.registerExtensionPoint(area, PsiElementFinder.EP_NAME, PsiElementFinder.class);
    }
    
    private static void registerApplicationExtensionPointsAndExtensionsFrom(String configFilePath) {
        File pluginRoot = new File(KOTLIN_COMPILER_PATH);
        CoreApplicationEnvironment.registerExtensionPointAndExtensions(pluginRoot, configFilePath, Extensions.getRootArea());
        
        CoreApplicationEnvironment.registerExtensionPoint(Extensions.getRootArea(), CodeStyleSettingsProvider.EXTENSION_POINT_NAME, KotlinSettingsProvider.class);
        CoreApplicationEnvironment.registerExtensionPoint(Extensions.getRootArea(), LanguageCodeStyleSettingsProvider.EP_NAME, KotlinLanguageCodeStyleSettingsProvider.class);
        Extensions.getRootArea().getExtensionPoint(CodeStyleSettingsProvider.EXTENSION_POINT_NAME).registerExtension(new KotlinSettingsProvider());
        Extensions.getRootArea().getExtensionPoint(LanguageCodeStyleSettingsProvider.EP_NAME).registerExtension(new KotlinLanguageCodeStyleSettingsProvider());
    }
    
    @NotNull
    public static KotlinEnvironment getEnvironment(@NotNull IJavaProject javaProject) {
        return cachedEnvironment.getOrCreateEnvironment(javaProject, environmentCreation);
    }
    
    public static void updateKotlinEnvironment(@NotNull IJavaProject javaProject) {
        cachedEnvironment.updateEnvironment(javaProject, environmentCreation);
    }
    
    @Nullable
    public static IJavaProject getJavaProject(@NotNull Project project) {
        return cachedEnvironment.getJavaProject(project);
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
        
        javaApplicationEnvironment.registerFileType(KotlinFileType.INSTANCE, "kt");
        javaApplicationEnvironment.registerFileType(KotlinFileType.INSTANCE, "jet");
        javaApplicationEnvironment.registerFileType(KotlinFileType.INSTANCE, "ktm");
        
        javaApplicationEnvironment.registerParserDefinition(new KotlinParserDefinition());
        
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
=======
        cachedEnvironment.putEnvironment(javaProject, this)
    }
    
    fun getRoots(): Set<VirtualFile> = roots

    private fun configureClasspath() {
        for (file in ProjectUtils.collectClasspathWithDependenciesForBuild(javaProject)) {
            addToClasspath(file)
        }
    }

    private fun createJavaCoreApplicationEnvironment(disposable: Disposable): JavaCoreApplicationEnvironment {
        Extensions.cleanRootArea(disposable)
        registerAppExtensionPoints()

        return JavaCoreApplicationEnvironment(disposable).apply {
            registerFileType(PlainTextFileType.INSTANCE, "xml")
            registerFileType(KotlinFileType.INSTANCE, "kt")
            registerFileType(KotlinFileType.INSTANCE, KotlinParserDefinition.STD_SCRIPT_SUFFIX)
            registerParserDefinition(KotlinParserDefinition())
            
            getApplication().registerService(KotlinBinaryClassCache::class.java, KotlinBinaryClassCache())
        }
>>>>>>> ca7e3f6... J2K Kotlin Environment: convert and prettify
    }

    fun getVirtualFile(location: IPath): VirtualFile? {
        return javaApplicationEnvironment.getLocalFileSystem().findFileByIoFile(location.toFile())
    }

    fun getVirtualFileInJar(pathToJar: IPath, relativePath: String): VirtualFile? {
        return javaApplicationEnvironment.getJarFileSystem().findFileByPath("$pathToJar!/$relativePath")
    }

    fun isJarFile(pathToJar: IPath): Boolean {
        val jarFile = javaApplicationEnvironment.getJarFileSystem().findFileByPath("$pathToJar!/")
        return jarFile != null && jarFile.isValid()
    }

    private fun addToClasspath(path: File) {
        if (path.isFile()) {
            val jarFile = javaApplicationEnvironment.getJarFileSystem().findFileByPath("$path!/")
            if (jarFile == null) {
                KotlinLogger.logWarning("Can't find jar: $path")
                return
            }
            
            projectEnvironment.addJarToClassPath(path)
            roots.add(jarFile)
        } else {
            val root = javaApplicationEnvironment.getLocalFileSystem().findFileByPath(path.getAbsolutePath())
            if (root == null) {
                KotlinLogger.logWarning("Can't find jar: $path")
                return
            }
            
            projectEnvironment.addSourcesToClasspath(root)
            roots.add(root)
        }
    }

    companion object {
        private val cachedEnvironment = CachedEnvironment()
        private val environmentCreation = {
            javaProject: IJavaProject -> KotlinEnvironment(javaProject, Disposer.newDisposable())
        }

        @JvmStatic fun getEnvironment(javaProject: IJavaProject): KotlinEnvironment {
            return cachedEnvironment.getOrCreateEnvironment(javaProject, environmentCreation)
        }

        @JvmStatic fun updateKotlinEnvironment(javaProject: IJavaProject) {
            cachedEnvironment.updateEnvironment(javaProject, environmentCreation)
        }

        @JvmStatic fun getJavaProject(project: Project): IJavaProject? = cachedEnvironment.getJavaProject(project)
    }
}

private fun registerProjectExtensionPoints(area: ExtensionsArea) {
    registerExtensionPoint(area, PsiTreeChangePreprocessor.EP_NAME, PsiTreeChangePreprocessor::class)
    registerExtensionPoint(area, PsiElementFinder.EP_NAME, PsiElementFinder::class)
}

private fun registerApplicationExtensionPointsAndExtensionsFrom(configFilePath: String) {
    val pluginRoot = File(KOTLIN_COMPILER_PATH)
    CoreApplicationEnvironment.registerExtensionPointAndExtensions(pluginRoot, configFilePath, Extensions.getRootArea())

    registerExtensionPointInRoot(CodeStyleSettingsProvider.EXTENSION_POINT_NAME, KotlinSettingsProvider::class)
    registerExtensionPointInRoot(LanguageCodeStyleSettingsProvider.EP_NAME, KotlinLanguageCodeStyleSettingsProvider::class)

    with(Extensions.getRootArea()) {
        getExtensionPoint(CodeStyleSettingsProvider.EXTENSION_POINT_NAME).registerExtension(KotlinSettingsProvider())
        getExtensionPoint(LanguageCodeStyleSettingsProvider.EP_NAME).registerExtension(KotlinLanguageCodeStyleSettingsProvider())
    }
}

private fun registerAppExtensionPoints() {
    registerExtensionPointInRoot(ContainerProvider.EP_NAME, ContainerProvider::class)
    registerExtensionPointInRoot(ClsCustomNavigationPolicy.EP_NAME, ClsCustomNavigationPolicy::class)
    registerExtensionPointInRoot(ClassFileDecompilers.EP_NAME, ClassFileDecompilers.Decompiler::class)

    // For j2k converter
    registerExtensionPointInRoot(PsiAugmentProvider.EP_NAME, PsiAugmentProvider::class)
    registerExtensionPointInRoot(JavaMainMethodProvider.EP_NAME, JavaMainMethodProvider::class)
}

private fun <T : Any> registerExtensionPoint(
        area: ExtensionsArea,
        extensionPointName: ExtensionPointName<T>,
        aClass: KClass<out T>) {
    CoreApplicationEnvironment.registerExtensionPoint(area, extensionPointName, aClass.java)
}

private fun <T : Any> registerExtensionPointInRoot(extensionPointName: ExtensionPointName<T>, aClass: KClass<out T>) {
    registerExtensionPoint(Extensions.getRootArea(), extensionPointName, aClass)
}