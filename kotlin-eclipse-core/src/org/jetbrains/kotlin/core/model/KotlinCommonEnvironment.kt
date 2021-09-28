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
@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.core.model

import com.intellij.codeInsight.*
import com.intellij.codeInsight.runner.JavaMainMethodProvider
import com.intellij.core.CoreApplicationEnvironment
import com.intellij.core.CoreJavaFileManager
import com.intellij.core.JavaCoreProjectEnvironment
import com.intellij.formatting.Formatter
import com.intellij.formatting.FormatterImpl
import com.intellij.formatting.KotlinLanguageCodeStyleSettingsProvider
import com.intellij.formatting.KotlinSettingsProvider
import com.intellij.lang.MetaLanguage
import com.intellij.lang.jvm.facade.JvmElementProvider
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.extensions.ExtensionsArea
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaModuleSystem
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.PsiManager
import com.intellij.psi.augment.PsiAugmentProvider
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider
import com.intellij.psi.compiled.ClassFileDecompilers
import com.intellij.psi.impl.PsiElementFinderImpl
import com.intellij.psi.impl.PsiTreeChangePreprocessor
import com.intellij.psi.impl.compiled.ClsCustomNavigationPolicy
import com.intellij.psi.impl.file.impl.JavaFileManager
import org.eclipse.core.runtime.IPath
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupport
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.cli.common.CliModuleVisibilityManagerImpl
import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesDynamicCompoundIndex
import org.jetbrains.kotlin.cli.jvm.index.SingleJavaFileRootsIndex
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.core.resolve.BuiltInsReferenceResolver
import org.jetbrains.kotlin.core.resolve.KotlinCacheServiceImpl
import org.jetbrains.kotlin.core.resolve.KotlinSourceIndex
import org.jetbrains.kotlin.core.utils.KotlinImportInserterHelper
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.extensions.DeclarationAttributeAltererExtension
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.j2k.J2KPostProcessingRegistrar
import org.jetbrains.kotlin.idea.j2k.J2KPostProcessingRegistrarImpl
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.load.kotlin.KotlinBinaryClassCache
import org.jetbrains.kotlin.load.kotlin.ModuleVisibilityManager
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.resolve.CodeAnalyzerInitializer
import org.jetbrains.kotlin.resolve.ModuleAnnotationsResolver
import org.jetbrains.kotlin.resolve.diagnostics.DiagnosticSuppressor
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade
import org.jetbrains.kotlin.resolve.jvm.diagnostics.DefaultErrorMessagesJvm
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionProvider
import org.jetbrains.kotlin.scripting.extensions.ScriptingResolveExtension
import java.io.File
import kotlin.reflect.KClass

private fun setIdeaIoUseFallback() {
    if (SystemInfo.isWindows) {
        val properties = System.getProperties()

        properties.setProperty("idea.io.use.nio2", java.lang.Boolean.TRUE.toString())

        if (!(SystemInfo.isJavaVersionAtLeast(1, 7, 0) && "1.7.0-ea" != SystemInfo.JAVA_VERSION)) {
            properties.setProperty("idea.io.use.fallback", java.lang.Boolean.TRUE.toString())
        }
    }
}

abstract class KotlinCommonEnvironment(disposable: Disposable) {
    val project: MockProject

    val projectEnvironment: JavaCoreProjectEnvironment
    private val roots = LinkedHashSet<JavaRoot>()

    val configuration = CompilerConfiguration()

    init {
        setIdeaIoUseFallback()

        projectEnvironment = object : JavaCoreProjectEnvironment(disposable, kotlinCoreApplicationEnvironment) {
            override fun preregisterServices() {
                registerProjectExtensionPoints(project.extensionArea)
                CoreApplicationEnvironment.registerExtensionPoint(
                    project.extensionArea,
                    JvmElementProvider.EP_NAME,
                    JvmElementProvider::class.java
                )
            }

            override fun createCoreFileManager() = KotlinCliJavaFileManagerImpl(PsiManager.getInstance(project))
        }

        project = projectEnvironment.getProject()
        DeclarationAttributeAltererExtension.registerExtensionPoint(project)
        StorageComponentContainerContributor.registerExtensionPoint(project)

        with(project) {
            val scriptDefinitionProvider = EclipseScriptDefinitionProvider()
            registerService(ScriptDefinitionProvider::class.java, scriptDefinitionProvider)
            registerService(ModuleVisibilityManager::class.java, CliModuleVisibilityManagerImpl(true))

            // For j2k converter
            registerService(NullableNotNullManager::class.java, KotlinNullableNotNullManager(project))

            registerService(
                CoreJavaFileManager::class.java,
                ServiceManager.getService(project, JavaFileManager::class.java) as CoreJavaFileManager
            )

            registerService(ModuleAnnotationsResolver::class.java, CliModuleAnnotationsResolver())
            registerService(CodeStyleManager::class.java, DummyCodeStyleManager())
            registerService(BuiltInsReferenceResolver::class.java, BuiltInsReferenceResolver(project))
            registerService(KotlinSourceIndex::class.java, KotlinSourceIndex())
            registerService(KotlinCacheService::class.java, KotlinCacheServiceImpl(project))
            registerService(ImportInsertHelper::class.java, KotlinImportInserterHelper())

            registerService(ExternalAnnotationsManager::class.java, MockExternalAnnotationsManager())
            registerService(InferredAnnotationsManager::class.java, MockInferredAnnotationsManager())

            val traceHolder = CliTraceHolder().also {
                registerService(CodeAnalyzerInitializer::class.java, it)
            }

            CliLightClassGenerationSupport(traceHolder, project).also {
                registerService(LightClassGenerationSupport::class.java, it)
                registerService(CliLightClassGenerationSupport::class.java, it)
            }

            registerService(JavaModuleResolver::class.java, EclipseKotlinJavaModuleResolver())

            val area = extensionArea
            val javaFileManager = ServiceManager.getService(this, JavaFileManager::class.java)
            (javaFileManager as KotlinCliJavaFileManagerImpl)
                .initialize(
                    JvmDependenciesDynamicCompoundIndex(),
                    arrayListOf(),
                    SingleJavaFileRootsIndex(arrayListOf()),
                    false
                )
            area.getExtensionPoint(PsiElementFinder.EP_NAME)
                .registerExtension(PsiElementFinderImpl(this, javaFileManager))
            val kotlinAsJavaSupport = CliKotlinAsJavaSupport(this, traceHolder)
            registerService(KotlinAsJavaSupport::class.java, kotlinAsJavaSupport)
            area.getExtensionPoint(PsiElementFinder.EP_NAME)
                .registerExtension(JavaElementFinder(this))
            area.getExtensionPoint(SyntheticResolveExtension.extensionPointName)
                .registerExtension(ScriptingResolveExtension())
            registerService(KotlinJavaPsiFacade::class.java, KotlinJavaPsiFacade(this))
        }

        configuration.put(CommonConfigurationKeys.MODULE_NAME, project.name)

        ExpressionCodegenExtension.Companion.registerExtensionPoint(project)
        registerApplicationExtensionPointsAndExtensionsFrom()

        ClassBuilderInterceptorExtension.registerExtensionPoint(project)
    }

    fun getRoots(): Set<JavaRoot> = roots

    fun getVirtualFile(location: IPath): VirtualFile? {
        return kotlinCoreApplicationEnvironment.localFileSystem.findFileByIoFile(location.toFile())
    }

    fun getVirtualFileInJar(pathToJar: IPath, relativePath: String): VirtualFile? {
        return kotlinCoreApplicationEnvironment.jarFileSystem.findFileByPath("$pathToJar!/$relativePath")
    }

    fun isJarFile(pathToJar: IPath): Boolean {
        val jarFile = kotlinCoreApplicationEnvironment.jarFileSystem.findFileByPath("$pathToJar!/")
        return jarFile != null && jarFile.isValid
    }

    protected fun addToClasspath(path: File, rootType: JavaRoot.RootType? = null) {
        if (path.isFile) {
            val jarFile = kotlinCoreApplicationEnvironment.jarFileSystem.findFileByPath("$path!/")
            if (jarFile == null) {
                KotlinLogger.logWarning("Can't find jar: $path")
                return
            }

            projectEnvironment.addJarToClassPath(path)

            val type = rootType ?: JavaRoot.RootType.BINARY
            roots.add(JavaRoot(jarFile, type))
        } else {
            val root = kotlinCoreApplicationEnvironment.localFileSystem.findFileByPath(path.absolutePath)
            if (root == null) {
                KotlinLogger.logWarning("Can't find jar: $path")
                return
            }

            projectEnvironment.addSourcesToClasspath(root)

            val type = rootType ?: JavaRoot.RootType.SOURCE
            roots.add(JavaRoot(root, type))
        }
    }

    companion object {
        val kotlinCoreApplicationEnvironment: KotlinCoreApplicationEnvironment by lazy {
            createKotlinCoreApplicationEnvironment(Disposer.newDisposable("Root Disposable"))
        }
    }
}

private fun createKotlinCoreApplicationEnvironment(disposable: Disposable): KotlinCoreApplicationEnvironment =
    KotlinCoreApplicationEnvironment.create(disposable, false).apply {
        registerAppExtensionPoints()

        registerFileType(PlainTextFileType.INSTANCE, "xml")
        registerFileType(KotlinFileType.INSTANCE, "kt")
        registerFileType(KotlinFileType.INSTANCE, KotlinParserDefinition.STD_SCRIPT_SUFFIX)
        registerParserDefinition(KotlinParserDefinition())

        application.registerService(Formatter::class.java, FormatterImpl())
        application.registerService(KotlinBinaryClassCache::class.java, KotlinBinaryClassCache())
        application.registerService(ScriptDefinitionProvider::class.java, EclipseScriptDefinitionProvider())
        application.registerService(J2KPostProcessingRegistrar::class.java, J2KPostProcessingRegistrarImpl)
    }

private fun registerProjectExtensionPoints(area: ExtensionsArea) {
    registerExtensionPoint(area, PsiTreeChangePreprocessor.EP_NAME, PsiTreeChangePreprocessor::class)
    registerExtensionPoint(area, PsiElementFinder.EP_NAME, PsiElementFinder::class)
    registerExtensionPoint(area, SyntheticResolveExtension.extensionPointName, SyntheticResolveExtension::class)
}

@Suppress("LocalVariableName")
private fun registerApplicationExtensionPointsAndExtensionsFrom() {
    val EP_ERROR_MSGS =
        ExtensionPointName.create<DefaultErrorMessages.Extension>("org.jetbrains.defaultErrorMessages.extension")
    registerExtensionPointInRoot(DiagnosticSuppressor.EP_NAME, DiagnosticSuppressor::class)
    registerExtensionPointInRoot(EP_ERROR_MSGS, DefaultErrorMessages.Extension::class)

    registerExtensionPointInRoot(CodeStyleSettingsProvider.EXTENSION_POINT_NAME, KotlinSettingsProvider::class)
    registerExtensionPointInRoot(
        LanguageCodeStyleSettingsProvider.EP_NAME,
        KotlinLanguageCodeStyleSettingsProvider::class
    )
    registerExtensionPointInRoot(JavaModuleSystem.EP_NAME, JavaModuleSystem::class)

    with(Extensions.getRootArea()) {
        getExtensionPoint(EP_ERROR_MSGS).registerExtension(DefaultErrorMessagesJvm())
        getExtensionPoint(CodeStyleSettingsProvider.EXTENSION_POINT_NAME).registerExtension(KotlinSettingsProvider())
        getExtensionPoint(LanguageCodeStyleSettingsProvider.EP_NAME).registerExtension(
            KotlinLanguageCodeStyleSettingsProvider()
        )
    }
}

private fun registerAppExtensionPoints() {
    registerExtensionPointInRoot(ContainerProvider.EP_NAME, ContainerProvider::class)
    registerExtensionPointInRoot(ClsCustomNavigationPolicy.EP_NAME, ClsCustomNavigationPolicy::class)
    //TODO
    //registerExtensionPointInRoot(ClassFileDecompilers.EP_NAME, ClassFileDecompilers.Decompiler::class)

    // For j2k converter
    registerExtensionPointInRoot(PsiAugmentProvider.EP_NAME, PsiAugmentProvider::class)
    registerExtensionPointInRoot(JavaMainMethodProvider.EP_NAME, JavaMainMethodProvider::class)

    CoreApplicationEnvironment.registerExtensionPoint(
        Extensions.getRootArea(),
        MetaLanguage.EP_NAME,
        MetaLanguage::class.java
    )
}

private fun <T : Any> registerExtensionPoint(
    area: ExtensionsArea,
    extensionPointName: ExtensionPointName<T>,
    aClass: KClass<out T>
) {
    CoreApplicationEnvironment.registerExtensionPoint(area, extensionPointName, aClass.java)
}

private fun <T : Any> registerExtensionPointInRoot(extensionPointName: ExtensionPointName<T>, aClass: KClass<out T>) {
    registerExtensionPoint(Extensions.getRootArea(), extensionPointName, aClass)
}
