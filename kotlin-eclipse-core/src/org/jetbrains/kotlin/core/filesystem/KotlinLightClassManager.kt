package org.jetbrains.kotlin.core.filesystem

import com.google.common.base.Predicate
import com.intellij.openapi.components.ServiceManager
import com.intellij.psi.PsiElement
import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IFolder
import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IResource
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.runtime.IPath
import org.eclipse.core.runtime.Path
import org.eclipse.jdt.core.IJavaProject
import org.jetbrains.kotlin.codegen.binding.PsiCodegenPredictor
import org.jetbrains.kotlin.core.asJava.LightClassFile
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.model.KotlinEnvironment
import org.jetbrains.kotlin.core.model.KotlinJavaManager
import org.jetbrains.kotlin.core.utils.ProjectUtils
import org.jetbrains.kotlin.fileClasses.NoResolveFileClassesProvider
import org.jetbrains.kotlin.fileClasses.getFileClassInternalName
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.KtVisitorVoid
import java.io.File
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import java.util.concurrent.ConcurrentHashMap

class KotlinLightClassManager(val javaProject: IJavaProject) {
    companion object {
        @JvmStatic fun getInstance(javaProject: IJavaProject): KotlinLightClassManager {
            val ideaProject = KotlinEnvironment.getEnvironment(javaProject).getProject()
            return ServiceManager.getService(ideaProject, KotlinLightClassManager::class.java)
        }
    }
    
    private val sourceFiles = ConcurrentHashMap<File, HashSet<IFile>>()

    fun computeLightClassesSources() {
        val project = javaProject.getProject()
        val newSourceFilesMap = HashMap<File, HashSet<IFile>>()
        
        for (sourceFile in KotlinPsiManager.INSTANCE.getFilesByProject(project)) {
            for (path in getLightClassesPaths(sourceFile)) {
                val lightClassFile = LightClassFile(project.getFile(path))
                
                val newSourceFiles = newSourceFilesMap.getOrPut(lightClassFile.asFile()) {
                    hashSetOf<IFile>()
                }
                newSourceFiles.add(sourceFile)
            }
        }
        
        sourceFiles.clear()
        sourceFiles.putAll(newSourceFilesMap)
    }

    fun updateLightClasses(affectedFiles: Set<IFile>) {
        val project = javaProject.getProject()
        for (entry in sourceFiles.entries) {
            val lightClassIFile = ResourcesPlugin.getWorkspace().getRoot().getFile(Path(entry.key.getPath()))
            if (lightClassIFile == null) continue
            
            val lightClassFile = LightClassFile(lightClassIFile)
            
            createParentDirsFor(lightClassFile)
            lightClassFile.createIfNotExists()
            
            for (sourceFile in entry.value) {
                if (affectedFiles.contains(sourceFile)) {
                    lightClassFile.touchFile()
                    break
                }
            }
        }
        
        cleanOutdatedLightClasses(project)
    }

    fun getSourceFiles(file: File): List<KtFile> {
        if (sourceFiles.isEmpty()) {
            computeLightClassesSources()
        }
        
        return getSourceKtFiles(file)
    }

    private fun getSourceKtFiles(lightClass: File): List<KtFile> {
        val sourceIOFiles = sourceFiles[lightClass] ?: return emptyList()
        return sourceIOFiles.mapNotNull { KotlinPsiManager.getKotlinParsedFile(it) }
    }

    private fun getLightClassesPaths(sourceFile: IFile): List<IPath> {
        val ktFile = KotlinPsiManager.INSTANCE.getParsedFile(sourceFile)
        
        val lightClasses = arrayListOf<IPath>()
        for (classOrObject in findLightClasses(ktFile)) {
            val internalName = PsiCodegenPredictor.getPredefinedJvmInternalName(classOrObject, NoResolveFileClassesProvider)
            if (internalName != null) {
                lightClasses.add(computePathByInternalName(internalName))
            }
        }
        
        if (PackagePartClassUtils.fileHasTopLevelCallables(ktFile)) {
            val newFacadeInternalName = NoResolveFileClassesProvider.getFileClassInternalName(ktFile)
            lightClasses.add(computePathByInternalName(newFacadeInternalName))
        }
        
        return lightClasses
    }

    private fun findLightClasses(ktFile: KtFile): List<KtClassOrObject> {
        val lightClasses = ArrayList<KtClassOrObject>()
        ktFile.acceptChildren(object : KtVisitorVoid() {
            override fun visitClassOrObject(classOrObject: KtClassOrObject) {
                lightClasses.add(classOrObject)
                super.visitClassOrObject(classOrObject)
            }

            override fun visitNamedFunction(function: KtNamedFunction) {
            }

            override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor) {
            }

            override fun visitProperty(property: KtProperty) {
            }

            override fun visitElement(element: PsiElement?) {
                if (element != null) {
                    element.acceptChildren(this)
                }
            }
        })
        
        return lightClasses
    }

    private fun computePathByInternalName(internalName: String): IPath {
        val relativePath = Path("$internalName.class")
        return KotlinJavaManager.KOTLIN_BIN_FOLDER.append(relativePath)
    }

    private fun cleanOutdatedLightClasses(project: IProject) {
        ProjectUtils.cleanFolder(KotlinJavaManager.getKotlinBinFolderFor(project), object : Predicate<IResource> {
            override fun apply(resource: IResource?): Boolean {
                if (resource is IFile) {
                    val lightClass = LightClassFile(resource)
                    val sources = sourceFiles[lightClass.asFile()]
                    
                    return if (sources != null) sources.isEmpty() else true
                } else if (resource is IFolder) {
                    return resource.members().isEmpty()
                }
                
                return false
            }
        })
    }

    private fun createParentDirsFor(lightClassFile: LightClassFile) {
        val parent = lightClassFile.getResource().getParent()
        if (parent is IFolder && !parent.exists()) {
            createParentDirs(parent)
        }
    }

    private fun createParentDirs(folder: IFolder?) {
        val parent = folder!!.getParent()
        if (!parent.exists()) {
            createParentDirs(parent as IFolder?)
        }
        
        folder.create(true, true, null)
    }
}