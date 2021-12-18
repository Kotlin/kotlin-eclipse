package org.jetbrains.kotlin.ui.builder

import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IResourceDelta
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.runtime.Status
import org.eclipse.core.runtime.jobs.Job
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.ui.PlatformUI
import org.jetbrains.kotlin.core.asJava.KotlinLightClassGeneration
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.model.KotlinScriptEnvironment
import org.jetbrains.kotlin.core.model.runJob
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaElementUtil
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.ui.KotlinPluginUpdater
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.jetbrains.kotlin.ui.editors.annotations.AnnotationManager
import org.jetbrains.kotlin.ui.editors.annotations.DiagnosticAnnotation
import org.jetbrains.kotlin.ui.editors.annotations.DiagnosticAnnotationUtil
import org.jetbrains.kotlin.ui.editors.quickfix.removeMarkers

abstract class BaseKotlinBuilderElement {

    private val fileFilters = listOf(ScriptFileFilter, FileFromOutputFolderFilter, FileFromKotlinBinFolderFilter)

    abstract fun build(project: IProject, delta: IResourceDelta?, kind: Int): Array<IProject>?

    protected fun isAllFilesApplicableForFilters(files: Set<IFile>, javaProject: IJavaProject): Boolean {
        return files.all { file ->
            fileFilters.any { filter ->
                filter.isApplicable(file, javaProject)
            }
        }
    }

    protected fun makeClean(javaProject: IJavaProject) {
        val kotlinFiles = KotlinPsiManager.getFilesByProject(javaProject.project)
        val existingFiles = kotlinFiles.filter { it.exists() }

        commitFiles(existingFiles)

        clearProblemAnnotationsFromOpenEditorsExcept(emptyList())
        clearMarkersFromFiles(existingFiles)

        runCancellableAnalysisFor(javaProject) { analysisResult ->
            updateLineMarkers(analysisResult.bindingContext.diagnostics, existingFiles)
            KotlinLightClassGeneration.updateLightClasses(javaProject.project, kotlinFiles)
        }
    }

    protected fun commitFiles(files: Collection<IFile>) {
        files.forEach { KotlinPsiManager.commitFile(it, EditorUtil.getDocument(it)) }
    }

    protected fun getAllAffectedFiles(resourceDelta: IResourceDelta): Set<IFile> {
        val affectedFiles = hashSetOf<IFile>()
        resourceDelta.accept { delta ->
            if (delta.kind == IResourceDelta.NO_CHANGE) return@accept false

            val resource = delta.resource
            if (resource is IFile) {
                affectedFiles.add(resource)
            } else {
                return@accept true
            }

            false
        }

        return affectedFiles
    }

    protected fun updateLineMarkers(diagnostics: Diagnostics, affectedFiles: List<IFile>) {
        clearMarkersFromFiles(affectedFiles)
        addMarkersToProject(DiagnosticAnnotationUtil.INSTANCE.handleDiagnostics(diagnostics), affectedFiles)
    }

    protected fun postBuild(delta: IResourceDelta?, javaProject: IJavaProject) {
        val allAffectedFiles = if (delta != null) getAllAffectedFiles(delta) else emptySet()

        if (allAffectedFiles.isNotEmpty()) {
            if (isAllFilesApplicableForFilters(allAffectedFiles, javaProject)) {
                return
            }
        }

        val kotlinAffectedFiles =
                allAffectedFiles
                        .filterTo(hashSetOf()) { KotlinPsiManager.isKotlinSourceFile(it, javaProject) }

        val existingAffectedFiles = kotlinAffectedFiles.filter { it.exists() }

        commitFiles(existingAffectedFiles)

        KotlinLightClassGeneration.updateLightClasses(javaProject.project, kotlinAffectedFiles)
        if (kotlinAffectedFiles.isNotEmpty()) {

            runJob("Checking for update", Job.DECORATE) {
                KotlinPluginUpdater.kotlinFileEdited()
                Status.OK_STATUS
            }
        }

        val ktFiles = existingAffectedFiles.map { KotlinPsiManager.getParsedFile(it) }

        val analysisResultWithProvider = if (ktFiles.isEmpty())
            KotlinAnalyzer.analyzeProject(javaProject.project)
        else
            KotlinAnalyzer.analyzeFiles(ktFiles)

        clearProblemAnnotationsFromOpenEditorsExcept(existingAffectedFiles)
        updateLineMarkers(analysisResultWithProvider.analysisResult.bindingContext.diagnostics, existingAffectedFiles)

        runCancellableAnalysisFor(javaProject) { analysisResult ->
            val projectFiles = KotlinPsiManager.getFilesByProject(javaProject.project)
            updateLineMarkers(
                    analysisResult.bindingContext.diagnostics,
                    (projectFiles - existingAffectedFiles.toSet()).toList()
            )
        }
    }
}

private fun clearMarkersFromFiles(files: List<IFile>) {
    files.forEach { it.removeMarkers() }
}

fun clearProblemAnnotationsFromOpenEditorsExcept(affectedFiles: List<IFile>) {
    for (window in PlatformUI.getWorkbench().getWorkbenchWindows()) {
        for (page in window.pages) {
            page.editorReferences
                .map { it.getEditor(false) }
                .filterIsInstance(KotlinFileEditor::class.java)
                .filterNot { it.eclipseFile in affectedFiles }
                .forEach {
                    AnnotationManager.removeAnnotations(it, AnnotationManager.ANNOTATION_ERROR_TYPE)
                    AnnotationManager.removeAnnotations(it, AnnotationManager.ANNOTATION_WARNING_TYPE)
                }
        }
    }
}

private fun addMarkersToProject(annotations: Map<IFile, List<DiagnosticAnnotation>>, affectedFiles: List<IFile>) {
    for (file in affectedFiles) {
        DiagnosticAnnotationUtil.INSTANCE.addParsingDiagnosticAnnotations(file, annotations)
        annotations[file]?.forEach { AnnotationManager.addProblemMarker(it, file) }
    }
}

interface KotlinFileFilterForBuild {
    fun isApplicable(file: IFile, javaProject: IJavaProject): Boolean
}

object ScriptFileFilter : KotlinFileFilterForBuild {
    override fun isApplicable(file: IFile, javaProject: IJavaProject): Boolean {
        return KotlinScriptEnvironment.isScript(file)
    }
}

object FileFromOutputFolderFilter : KotlinFileFilterForBuild {
    override fun isApplicable(file: IFile, javaProject: IJavaProject): Boolean {
        val workspaceLocation = ResourcesPlugin.getWorkspace().getRoot().getFullPath()
        val outputLocation = javaProject.outputLocation

        val filePathLocation = file.fullPath.makeRelativeTo(workspaceLocation)
        return outputLocation.isPrefixOf(filePathLocation)
    }
}

object FileFromKotlinBinFolderFilter : KotlinFileFilterForBuild {
    override fun isApplicable(file: IFile, javaProject: IJavaProject): Boolean {
        return EclipseJavaElementUtil.isFromKotlinBinFolder(file)
    }
}