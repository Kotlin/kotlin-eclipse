/*******************************************************************************
 * Copyright 2000-2015 JetBrains s.r.o.
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
package org.jetbrains.kotlin.ui.editors.occurrences

import org.eclipse.ui.ISelectionListener
import org.eclipse.jface.viewers.ISelection
import org.eclipse.ui.IWorkbenchPart
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.eclipse.jface.text.ITextSelection
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.ui.search.KotlinQueryParticipant
import org.eclipse.search.ui.text.Match
import org.eclipse.jdt.internal.ui.search.JavaSearchScopeFactory
import org.eclipse.jdt.core.search.IJavaSearchConstants
import org.jetbrains.kotlin.core.references.resolveToSourceDeclaration
import org.jetbrains.kotlin.ui.commands.findReferences.KotlinScopedQuerySpecification
import org.eclipse.core.runtime.NullProgressMonitor
import org.jetbrains.kotlin.ui.search.KotlinElementMatch
import org.jetbrains.kotlin.ui.refactorings.rename.getLengthOfIdentifier
import org.jetbrains.kotlin.eclipse.ui.utils.getTextDocumentOffset
import org.eclipse.jface.text.TextSelection
import org.eclipse.jface.text.Position
import org.eclipse.jface.text.source.Annotation
import org.eclipse.jface.text.source.IAnnotationModel
import org.eclipse.jface.text.ISynchronizable
import org.eclipse.jface.text.source.IAnnotationModelExtension
import org.eclipse.core.runtime.jobs.Job
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Status
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.resources.IFile
import org.jetbrains.kotlin.ui.editors.annotations.withLock
import org.eclipse.jface.text.source.AnnotationModel
import org.eclipse.ui.progress.UIJob

public class KotlinMarkOccurrences : ISelectionListener {
    companion object {
        private val ANNOTATION_TYPE = "org.eclipse.jdt.ui.occurrences"
    }
    
    private @Volatile var previousElement: KtElement? = null
    
    override fun selectionChanged(part: IWorkbenchPart, selection: ISelection) {
        val job = object : Job("Update occurrence annotations") {
            override fun run(monitor: IProgressMonitor?): IStatus? {
                if (part is KotlinFileEditor && selection is ITextSelection) {
                    val jetElement = EditorUtil.getJetElement(part, selection.getOffset())
                    if (jetElement == null || jetElement == previousElement) {
                        previousElement = null
                        return Status.CANCEL_STATUS
                    } else {
                        previousElement = jetElement
                    }
                    
                    val file = part.getFile()
                    if (file == null) return Status.CANCEL_STATUS
                    
                    val occurrences = findOccurrences(part, jetElement, file)
                    updateOccurrences(part, occurrences)
                }
                
                return Status.OK_STATUS
            }
        }
        
        job.setPriority(Job.DECORATE)
        job.schedule()
    }
    
    private fun updateOccurrences(editor: KotlinFileEditor, occurrences: List<Position>) {
        val annotationMap = occurrences.toMapBy { Annotation(ANNOTATION_TYPE, false, "description") }
        val annotationModel = editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput())
        val oldAnnotations = getOldOccurrenceAnnotations(annotationModel)
        annotationModel.withLock { 
            (annotationModel as IAnnotationModelExtension).replaceAnnotations(oldAnnotations.toTypedArray(), annotationMap)
        }
    }
    
    private fun getOldOccurrenceAnnotations(model: IAnnotationModel): List<Annotation> {
        val annotations = arrayListOf<Annotation>()
        for (annotation in model.getAnnotationIterator()) {
            if (annotation is Annotation && annotation.getType() == ANNOTATION_TYPE) {
                annotations.add(annotation)
            }
        }
        
        return annotations
    }
    
    private fun findOccurrences(editor: KotlinFileEditor, jetElement: KtElement, file: IFile): List<Position> {
        val sourceElements = jetElement.resolveToSourceDeclaration(editor.javaProject!!)
        if (sourceElements.isEmpty()) return emptyList()
        
        val querySpecification = KotlinScopedQuerySpecification(sourceElements, listOf(file), 
                IJavaSearchConstants.ALL_OCCURRENCES, "Searching in ${file.getName()}")
        
        val occurrences = arrayListOf<Match>()
        KotlinQueryParticipant().search({ occurrences.add(it) }, querySpecification, NullProgressMonitor())
        
        return occurrences.map { 
            if (it !is KotlinElementMatch) return@map null
            
            val element = it.jetElement
            val length = getLengthOfIdentifier(element)
            if (length == null) return@map null
            
            Position(element.getTextDocumentOffset(editor.document), length)
        }.filterNotNull()
    }
}