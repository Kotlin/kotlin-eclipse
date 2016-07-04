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
package org.jetbrains.kotlin.core.builder

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.util.text.StringUtilRt
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IResource
import org.eclipse.core.resources.IResourceDelta
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.runtime.CoreException
import org.eclipse.core.runtime.Path
import org.eclipse.jdt.core.IClasspathEntry
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jface.text.IDocument
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.core.model.KotlinLightVirtualFile
import org.jetbrains.kotlin.core.model.KotlinNature
import org.jetbrains.kotlin.core.model.KotlinScriptEnvironment
import org.jetbrains.kotlin.core.model.getEnvironment
import org.jetbrains.kotlin.core.utils.ProjectUtils
import org.jetbrains.kotlin.core.utils.sourceFolders
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import java.io.File
import java.util.Collections
import java.util.HashSet
import java.util.concurrent.ConcurrentHashMap

interface PsiFilesStorage {
    fun getPsiFile(eclipseFile: IFile): KtFile
    
    fun getPsiFile(file: IFile, expectedSourceCode: String): KtFile
    
    fun isApplicable(file: IFile): Boolean
    
    fun removeFile(file: IFile)
}

private class ScriptsFilesStorage : PsiFilesStorage {
    private val cachedKtFiles = ConcurrentHashMap<IFile, KtFile>()
    
    override fun getPsiFile(eclipseFile: IFile): KtFile {
        assert(isApplicable(eclipseFile)) { "$eclipseFile is not applicable for Kotlin scripts storage" }
        
        return cachedKtFiles.getOrPut(eclipseFile) { KotlinPsiManager.parseFile(eclipseFile)!! }
    }

    @Synchronized
    override fun getPsiFile(file: IFile, expectedSourceCode: String): KtFile {
        val sourceCodeWithouCR = StringUtilRt.convertLineSeparators(expectedSourceCode)
        
        val currentParsedFile = getPsiFile(file)
        if (currentParsedFile.getText() != sourceCodeWithouCR) {
            val jetFile = KotlinPsiManager.parseText(sourceCodeWithouCR, file)!!
            cachedKtFiles.put(file, jetFile)
        }
        
        return getPsiFile(file)
    }

    override fun isApplicable(file: IFile): Boolean = KotlinScriptEnvironment.isScript(file)
    
    override fun removeFile(file: IFile) {
        cachedKtFiles.remove(file)
    }
}

private class ProjectSourceFiles : PsiFilesStorage {
    companion object {
        @JvmStatic
        fun isKotlinFile(file: IFile): Boolean = KotlinFileType.INSTANCE.getDefaultExtension() == file.fileExtension
    }
    
    private val projectFiles = hashMapOf<IProject, HashSet<IFile>>()
    private val cachedKtFiles = hashMapOf<IFile, KtFile>()
    private val mapOperationLock = Any()
    
    override fun getPsiFile(eclipseFile: IFile): KtFile {
        synchronized (mapOperationLock) {
            updateProjectPsiSourcesIfNeeded(eclipseFile.getProject())

            assert(existsInProjectSources(eclipseFile), { "File(" + eclipseFile.getName() + ") does not contain in the psiFiles" })

            return cachedKtFiles.getOrPut(eclipseFile) { KotlinPsiManager.parseFile(eclipseFile)!! }
        }
    }
    
    override fun getPsiFile(file: IFile, expectedSourceCode: String): KtFile {
        synchronized (mapOperationLock) {
            updatePsiFile(file, expectedSourceCode)
            return getPsiFile(file)
        }
    }
    
    override fun isApplicable(file: IFile): Boolean = existsInProjectSources(file)

    fun existsInProjectSources(file: IFile): Boolean {
        synchronized (mapOperationLock) {
            val project = file.getProject() ?: return false
            
            updateProjectPsiSourcesIfNeeded(project)
            
            val files = projectFiles[project]
            return if (files != null) files.contains(file) else false
        }
    }
    
    fun containsProject(project: IProject): Boolean {
        return synchronized (mapOperationLock) {
            projectFiles.containsKey(project)
        }
    }
    
    fun getFilesByProject(project: IProject): Set<IFile> {
        synchronized (mapOperationLock) {
            updateProjectPsiSourcesIfNeeded(project)
            
            if (projectFiles.containsKey(project)) {
                return Collections.unmodifiableSet(projectFiles[project])
            }
            
            return emptySet()
        }
    }
    
    fun addFile(file: IFile) {
        synchronized (mapOperationLock) {
            assert(KotlinNature.hasKotlinNature(file.getProject()),
                    { "Project (" + file.getProject().getName() + ") does not have Kotlin nature" })
            
            assert(!existsInProjectSources(file), { "File(" + file.getName() + ") is already added" })
            
            projectFiles
                    .getOrPut(file.project) { hashSetOf<IFile>() }
                    .add(file)
        }
    }
    
    override fun removeFile(file: IFile) {
        synchronized (mapOperationLock) {
            assert(existsInProjectSources(file), { "File(" + file.getName() + ") does not contain in the psiFiles" })
            
            cachedKtFiles.remove(file)
            projectFiles.get(file.project)?.remove(file)
        }
    }
    
    fun addProject(project: IProject) {
        synchronized (mapOperationLock) {
            if (ProjectUtils.isAccessibleKotlinProject(project)) {
                addFilesToParse(JavaCore.create(project))
            }
        }
    }
    
    fun removeProject(project: IProject) {
        synchronized (mapOperationLock) {
            val files = getFilesByProject(project)
            
            projectFiles.remove(project)
            for (file in files) {
                cachedKtFiles.remove(file)
            }
        }
    }
    
    fun addFilesToParse(javaProject: IJavaProject) {
        try {
            projectFiles.put(javaProject.getProject(), HashSet<IFile>())
            
            for (sourceFolder in javaProject.sourceFolders) {
                sourceFolder.getResource().accept { resource ->
                    if (resource is IFile && isKotlinFile(resource)) {
                        addFile(resource)
                    }
                    
                    true
                }
            }
        } catch (e: CoreException) {
            KotlinLogger.logError(e)
        }
    }
    
    fun updateProjectPsiSourcesIfNeeded(project: IProject) {
        if (projectFiles.containsKey(project)) {
            return
        }
        
        if (ProjectUtils.isAccessibleKotlinProject(project)) {
            updateProjectPsiSources(project, IResourceDelta.ADDED)
        }
    }
    
    fun updateProjectPsiSources(project: IProject, flag: Int) {
        when (flag) {
            IResourceDelta.ADDED -> addProject(project)
            IResourceDelta.REMOVED -> removeProject(project)
        }
    }
    
    private fun updatePsiFile(file: IFile, sourceCode: String) {
        val sourceCodeWithouCR = StringUtilRt.convertLineSeparators(sourceCode)
        
        synchronized (mapOperationLock) {
            assert(existsInProjectSources(file), { "File(" + file.getName() + ") does not contain in the psiFiles" })
            
            val currentParsedFile = getPsiFile(file)
            if (!currentParsedFile.getText().equals(sourceCodeWithouCR)) {
                val jetFile = KotlinPsiManager.parseText(sourceCodeWithouCR, file)!!
                cachedKtFiles.put(file, jetFile)
            }
        }
    }
}

object KotlinPsiManager {
    private val projectSourceFiles = ProjectSourceFiles()
    private val scriptsFiles = ScriptsFilesStorage()
    
    fun getParsedFile(file: IFile): KtFile {
        return storage(file).getPsiFile(file)
    }
    
    private fun isApplicable(file: IFile): Boolean {
        return storage(file).isApplicable(file)
    }
    
    fun updateProjectPsiSources(file: IFile, flag: Int) {
        when (flag) {
            IResourceDelta.ADDED -> projectSourceFiles.addFile(file)
            IResourceDelta.REMOVED -> storage(file).removeFile(file)
            else -> throw IllegalArgumentException()
        }
    }

    fun removeProjectFromManager(project: IProject) {
        projectSourceFiles.updateProjectPsiSources(project, IResourceDelta.REMOVED)
    }

    fun getFilesByProject(project: IProject): Set<IFile> {
        return projectSourceFiles.getFilesByProject(project)
    }

    fun existsSourceFile(file: IFile): Boolean {
        return projectSourceFiles.existsInProjectSources(file)
    }

    fun getFilesByProject(projectName: String): Set<IFile> {
        val project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName)
        
        updateProjectPsiSourcesIfNeeded(project)
        
        return getFilesByProject(project)
    }
    
    private fun storage(file: IFile): PsiFilesStorage {
        return when {
            scriptsFiles.isApplicable(file) -> scriptsFiles
            projectSourceFiles.isApplicable(file) -> projectSourceFiles
            else -> throw IllegalStateException("There is not applicable storage for file: $file")
        }
    }
    
    private fun updateProjectPsiSourcesIfNeeded(project: IProject) {
        projectSourceFiles.updateProjectPsiSourcesIfNeeded(project)
    }

    private fun getParsedFile(file: IFile, expectedSourceCode: String): KtFile {
        return storage(file).getPsiFile(file, expectedSourceCode)
    }

    @JvmOverloads
    @JvmStatic
    fun isKotlinSourceFile(
            resource: IResource,
            javaProject: IJavaProject = JavaCore.create(resource.project)): Boolean {
        
        if (!(resource is IFile) || !KotlinFileType.INSTANCE.getDefaultExtension().equals(resource.getFileExtension())) {
            return false
        }
        if (!javaProject.exists()) {
            return false
        }
        if (!KotlinNature.hasKotlinNature(javaProject.getProject())) {
            return false
        }
        
        val classpathEntries = javaProject.getRawClasspath()
        val resourceRoot = resource.getFullPath().segment(1)
        return classpathEntries.any { classpathEntry ->
            classpathEntry.entryKind == IClasspathEntry.CPE_SOURCE && resourceRoot == classpathEntry.path.segment(1)
        }
    }
    
    fun parseFile(file: IFile): KtFile? {
        if (!file.exists()) {
            return null
        }
        
        val ioFile = File(file.getRawLocation().toOSString())
        return parseText(FileUtil.loadFile(ioFile, null, true), file)
    }
    
    fun parseText(text: String, file: IFile): KtFile? {
        StringUtil.assertValidSeparators(text)
        
        val virtualFile = KotlinLightVirtualFile(file, text)
        virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET)
        
        val project = getEnvironment(file).project
        val psiFileFactory = PsiFileFactory.getInstance(project) as PsiFileFactoryImpl
        
        return psiFileFactory.trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, true, false) as? KtFile
    }

    @JvmStatic
    fun isKotlinFile(file: IFile): Boolean = KotlinFileType.INSTANCE.getDefaultExtension() == file.fileExtension

    @JvmStatic
    fun getKotlinParsedFile(file: IFile): KtFile? {
        return if (isApplicable(file)) getParsedFile(file) else null
    }

    @JvmStatic
    fun getKotlinFileIfExist(file: IFile, sourceCode: String): KtFile? {
        return if (isApplicable(file)) getParsedFile(file, sourceCode) else null
    }

    @JvmStatic
    fun getEclipseFile(jetFile: KtFile): IFile? {
        val virtualFile = jetFile.getVirtualFile()
        return if (virtualFile != null)
            ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(Path(virtualFile.getPath()))
        else
            null
    }

    @JvmStatic
    fun getJavaProject(jetElement: KtElement): IJavaProject? {
        return getEclipseFile(jetElement.getContainingKtFile())?.let { eclipseFile ->
            JavaCore.create(eclipseFile.project)
        }
    }

    @JvmStatic
    fun commitFile(file: IFile, document: IDocument) {
        getKotlinFileIfExist(file, document.get())
    }
}