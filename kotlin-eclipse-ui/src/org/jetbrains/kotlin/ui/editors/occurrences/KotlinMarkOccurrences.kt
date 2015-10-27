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
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.ui.search.KotlinQueryParticipant
import org.eclipse.search.ui.text.Match
import org.eclipse.jdt.internal.ui.search.JavaSearchScopeFactory
import org.eclipse.jdt.core.search.IJavaSearchConstants
import org.jetbrains.kotlin.core.references.resolveToSourceDeclaration
import org.jetbrains.kotlin.ui.commands.findReferences.KotlinQuerySpecification
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
import org.jetbrains.kotlin.ui.editors.withLock

public class KotlinMarkOccurrences(val editor: KotlinFileEditor) : ISelectionListener {
    private @Volatile var occurrenceAnnotations = setOf<Annotation>()
    
    override fun selectionChanged(part: IWorkbenchPart, selection: ISelection) {
        val job = object : Job("Mark occurrences") {
            override fun run(monitor: IProgressMonitor?): IStatus? {
                if (part is KotlinFileEditor && selection is ITextSelection) {
                    val jetElement = EditorUtil.getJetElement(part, selection.getOffset())
                    if (jetElement == null) return Status.CANCEL_STATUS
                            
                    val file = part.getFile()
                    if (file == null) return Status.CANCEL_STATUS
                    
                    val occurrences = findOccurrences(jetElement, file)
                    updateOccurrences(occurrences)
                }
                
                return Status.OK_STATUS
            }
        }
        
        job.schedule()
    }
    
    private @Synchronized fun updateOccurrences(occurrences: List<Position>) {
        val annotationMap = occurrences.toMap { Annotation("org.eclipse.jdt.ui.occurrences", false, "description") }
        val annotationModel = editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput())
        annotationModel.withLock { 
            (annotationModel as IAnnotationModelExtension).replaceAnnotations(occurrenceAnnotations.toTypedArray(), annotationMap)
            occurrenceAnnotations = annotationMap.keySet()
        }
    }
    
    private @Synchronized fun findOccurrences(jetElement: JetElement, file: IFile): List<Position> {
        val sourceElements = jetElement.resolveToSourceDeclaration(editor.javaProject!!)
        if (sourceElements.isEmpty()) return emptyList()
        
        val querySpecification = KotlinQuerySpecification(sourceElements, listOf(file), 
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