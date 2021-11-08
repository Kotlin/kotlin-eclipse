package org.jetbrains.kotlin.core.filesystem

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.eclipse.core.internal.jobs.JobStatus
import org.eclipse.core.resources.*
import org.eclipse.core.runtime.*
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.internal.core.util.LRUCache
import org.jetbrains.kotlin.core.asJava.LightClassFile
import org.jetbrains.kotlin.core.builder.KotlinPsiManager.getFilesByProject
import org.jetbrains.kotlin.core.builder.KotlinPsiManager.getKotlinParsedFile
import org.jetbrains.kotlin.core.builder.KotlinPsiManager.getParsedFile
import org.jetbrains.kotlin.core.log.KotlinLogger.logAndThrow
import org.jetbrains.kotlin.core.model.KotlinEnvironment.Companion.getEnvironment
import org.jetbrains.kotlin.core.model.KotlinJavaManager
import org.jetbrains.kotlin.core.model.KotlinJavaManager.getKotlinBinFolderFor
import org.jetbrains.kotlin.core.utils.ProjectUtils.cleanFolder
import org.jetbrains.kotlin.core.utils.ProjectUtils.getAllOutputFolders
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlin.text.Charsets.UTF_8

class KotlinLightClassManager(private val project: IProject) {
    private val cachedLightClasses = LRUCache<File, ByteArray>(LIGHT_CLASSES_CACHE_SIZE)
    private val sourceFiles: ConcurrentMap<File, MutableSet<IFile>> = ConcurrentHashMap()

    @Synchronized
    fun getCachedLightClass(file: File): ByteArray? {
        val lightClass: Any? = cachedLightClasses[file]
        return if (lightClass != null) lightClass as ByteArray? else null
    }

    @Synchronized
    fun cacheLightClass(file: File, lightClass: ByteArray) {
        cachedLightClasses.put(file, lightClass)
    }

    @Synchronized
    fun removeLightClass(file: File) {
        cachedLightClasses.flush(file)
        val tempFolders = getAllOutputFolders(JavaCore.create(project))
        val tempSegments = file.path.split("[/\\\\]".toRegex()).toTypedArray()
        val tempRealPath = tempSegments.copyOfRange(3, tempSegments.size)
        for (tempFolder in tempFolders) {
            val tempRootFolder = tempFolder.location.toFile()
            var tempCurrentFolder = tempRootFolder
            for ((tempIndex, tempSegment) in tempRealPath.withIndex()) {
                if (tempIndex == tempRealPath.lastIndex) {
                    val tempFile = File(tempCurrentFolder, tempSegment).takeIf { it.exists() } ?: break
                    val tempTouchedFilesFile = File(tempRootFolder, KOTLIN_TOUCHED_FILES_FILE_NAME)
                    try {
                        if (!tempTouchedFilesFile.exists()) tempTouchedFilesFile.createNewFile()

                        val tempLines = tempTouchedFilesFile.readLines(UTF_8).toMutableSet()
                        tempLines.add(tempFile.absolutePath)
                        tempTouchedFilesFile.writeText(tempLines.joinToString("\n"), UTF_8)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                } else {
                    tempCurrentFolder = File(tempCurrentFolder, tempSegment).takeIf { it.exists() } ?: break
                }
            }
        }
    }

    fun computeLightClassesSources() {
        val newSourceFilesMap: MutableMap<File, MutableSet<IFile>> = HashMap()
        for (sourceFile in getFilesByProject(project)) {
            val lightClassesPaths = getLightClassesPaths(sourceFile)
            for (path in lightClassesPaths) {
                val lightClassFile = LightClassFile(project.getFile(path))
                val newSourceFiles = newSourceFilesMap.computeIfAbsent(lightClassFile.asFile()) { HashSet() }
                newSourceFiles.add(sourceFile)
            }
        }
        sourceFiles.clear()
        sourceFiles.putAll(newSourceFilesMap)
    }

    fun updateLightClasses(affectedFiles: Set<IFile?>, resourceTreeBlocked: Boolean) {
        val toCreate: MutableList<LightClassFile> = ArrayList()
        val toRemove: MutableList<LightClassFile> = ArrayList()
        for ((key, value) in sourceFiles) {
            val lightClassIFile = ResourcesPlugin.getWorkspace().root.getFile(Path(key.path))
                    ?: continue
            val lightClassFile = LightClassFile(lightClassIFile)
            if (!lightClassFile.exists()) {
                toCreate.add(lightClassFile)
            }
            for (sourceFile in value) {
                if (affectedFiles.contains(sourceFile)) {
                    toRemove.add(lightClassFile)
                    break
                }
            }
        }
        if (resourceTreeBlocked) {
            if (toCreate.isNotEmpty() || toRemove.isNotEmpty()) {
                val job: WorkspaceJob = object : WorkspaceJob(WORKSPACE_JOB_ID) {
                    override fun runInWorkspace(monitor: IProgressMonitor): IStatus {
                        monitor.beginTask("Light class generation started", 0)
                        updateLightClasses(toCreate, toRemove)
                        monitor.done()
                        return JobStatus(0, this, "Light classes generation finished")
                    }
                }
                job.rule = ResourcesPlugin.getWorkspace().ruleFactory.createRule(
                        project.getFolder(KotlinJavaManager.KOTLIN_BIN_FOLDER))
                job.schedule()
            }
        } else {
            updateLightClasses(toCreate, toRemove)
        }
    }

    private fun updateLightClasses(toCreate: List<LightClassFile>, toRemove: List<LightClassFile>) {
        for (lightClassFile in toCreate) {
            createParentDirsFor(lightClassFile)
            lightClassFile.createIfNotExists()
        }
        for (lightClassFile in toRemove) {
            removeLightClass(lightClassFile.asFile())
            lightClassFile.touchFile()
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
        val sourceIOFiles: Set<IFile>? = sourceFiles[lightClass]
        if (sourceIOFiles != null) {
            val jetSourceFiles: MutableList<KtFile> = ArrayList()
            for (sourceFile in sourceIOFiles) {
                val jetFile = getKotlinParsedFile(sourceFile)
                if (jetFile != null) {
                    jetSourceFiles.add(jetFile)
                }
            }
            return jetSourceFiles
        }
        return emptyList()
    }

    private fun getLightClassesPaths(sourceFile: IFile): List<IPath> {
        val lightClasses: MutableList<IPath> = ArrayList()
        val ktFile = getParsedFile(sourceFile)
        for (classOrObject in findLightClasses(ktFile)) {
            val internalName = getInternalName(classOrObject)
            if (internalName != null) {
                lightClasses.add(computePathByInternalName(internalName))
            }
        }
        if (ktFile.hasTopLevelCallables()) {
            val newFacadeInternalName = JvmFileClassUtil.getFileClassInternalName(ktFile)
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

            override fun visitNamedFunction(function: KtNamedFunction) {}
            override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor) {}
            override fun visitProperty(property: KtProperty) {}
            override fun visitElement(element: PsiElement) {
                element.acceptChildren(this)
            }
        })
        return lightClasses
    }

    private fun computePathByInternalName(internalName: String): IPath {
        val relativePath = Path("$internalName.class")
        return KotlinJavaManager.KOTLIN_BIN_FOLDER.append(relativePath)
    }

    private fun cleanOutdatedLightClasses(project: IProject) {
        cleanFolder(getKotlinBinFolderFor(project)) { resource: IResource? ->
            if (resource is IFile) {
                val lightClass = LightClassFile(resource)
                val sources: Set<IFile>? = sourceFiles[lightClass.asFile()]
                val dropLightClass = sources == null || sources.isEmpty()
                if (dropLightClass) {
                    removeLightClass(lightClass.asFile())
                }
                return@cleanFolder dropLightClass
            } else if (resource is IFolder) {
                try {
                    return@cleanFolder resource.members().isEmpty()
                } catch (e: CoreException) {
                    logAndThrow(e)
                }
            }
            false
        }
    }

    private fun createParentDirsFor(lightClassFile: LightClassFile) {
        val parent = lightClassFile.resource.parent as? IFolder
        if (parent != null && !parent.exists()) {
            createParentDirs(parent)
        }
    }

    private fun createParentDirs(folder: IFolder) {
        val parent = folder.parent
        if (!parent.exists()) {
            createParentDirs(parent as IFolder)
        }
        try {
            folder.create(true, true, null)
        } catch (e: CoreException) {
            logAndThrow(e)
        }
    }

    companion object {
        const val KOTLIN_TOUCHED_FILES_FILE_NAME = "META-INF/kotlinTouchedFiles"

        private const val LIGHT_CLASSES_CACHE_SIZE = 300
        private const val WORKSPACE_JOB_ID = "updateLightClassesJob"
        fun getInstance(project: IProject): KotlinLightClassManager {
            val ideaProject: Project = getEnvironment(project).project
            return ServiceManager.getService(ideaProject, KotlinLightClassManager::class.java)
        }

        fun getInternalName(classOrObject: KtClassOrObject): String? {
            val fullFqName = classOrObject.fqName ?: return null
            val topmostClassOrObject = PsiTreeUtil.getTopmostParentOfType(classOrObject, KtClassOrObject::class.java)
                    ?: return makeInternalByToplevel(fullFqName)
            val topLevelFqName = topmostClassOrObject.fqName ?: return null
            val nestedPart = fullFqName.asString().substring(topLevelFqName.asString().length).replace("\\.".toRegex(), "\\$")
            return makeInternalByToplevel(topLevelFqName) + nestedPart
        }

        private fun makeInternalByToplevel(fqName: FqName): String {
            return fqName.asString().replace("\\.".toRegex(), "/")
        }
    }
}