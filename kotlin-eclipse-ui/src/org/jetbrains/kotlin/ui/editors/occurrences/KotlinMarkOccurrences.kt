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

import org.eclipse.core.resources.IFile
import org.eclipse.core.runtime.NullProgressMonitor
import org.eclipse.core.runtime.Status
import org.eclipse.core.runtime.jobs.Job
import org.eclipse.jdt.core.search.IJavaSearchConstants
import org.eclipse.jface.text.ITextSelection
import org.eclipse.jface.text.Position
import org.eclipse.jface.text.source.Annotation
import org.eclipse.jface.viewers.ISelection
import org.eclipse.search.ui.text.Match
import org.eclipse.ui.ISelectionListener
import org.eclipse.ui.IWorkbenchPart
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.model.runJob
import org.jetbrains.kotlin.core.references.resolveToSourceDeclaration
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.jetbrains.kotlin.eclipse.ui.utils.getTextDocumentOffset
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.ui.commands.findReferences.KotlinScopedQuerySpecification
import org.jetbrains.kotlin.ui.editors.KotlinCommonEditor
import org.jetbrains.kotlin.ui.editors.KotlinEditor
import org.jetbrains.kotlin.ui.editors.annotations.AnnotationManager
import org.jetbrains.kotlin.ui.refactorings.rename.getLengthOfIdentifier
import org.jetbrains.kotlin.ui.search.KotlinElementMatch
import org.jetbrains.kotlin.ui.search.KotlinQueryParticipant
import org.jetbrains.kotlin.ui.search.getContainingClassOrObjectForConstructor

class KotlinMarkOccurrences(private val kotlinEditor: KotlinCommonEditor) : ISelectionListener {
    companion object {
        private const val ANNOTATION_TYPE = "org.eclipse.jdt.ui.occurrences"
    }
    
    override fun selectionChanged(part: IWorkbenchPart, selection: ISelection) {
        if (!kotlinEditor.isActive()) return
        
        runJob("Update occurrence annotations", Job.DECORATE) { 
            if (part is KotlinCommonEditor && selection is ITextSelection) {
                val file = part.eclipseFile
                if (file == null || !file.exists()) return@runJob Status.CANCEL_STATUS
                
                val document = part.getDocumentSafely() ?: return@runJob Status.CANCEL_STATUS

                KotlinPsiManager.getKotlinFileIfExist(file, document.get())
                
                val ktElement = EditorUtil.getJetElement(part, selection.offset) ?: return@runJob Status.CANCEL_STATUS

                val occurrences = findOccurrences(part, ktElement, file)
                updateOccurrences(part, occurrences)
            }
            
            Status.OK_STATUS
        }
    }
    
    private fun updateOccurrences(editor: KotlinEditor, occurrences: List<Position>) {
        val annotationMap = occurrences.associateBy { Annotation(ANNOTATION_TYPE, false, null) }
        AnnotationManager.updateAnnotations(editor, annotationMap, ANNOTATION_TYPE)
    }
    
    private fun findOccurrences(editor: KotlinCommonEditor, jetElement: KtElement, file: IFile): List<Position> {
        val sourceElements = jetElement.resolveToSourceDeclaration()
        if (sourceElements.isEmpty()) return emptyList()
        
        val searchingElements = getSearchingElements(sourceElements)
        
        val querySpecification = KotlinScopedQuerySpecification(searchingElements, listOf(file), 
                IJavaSearchConstants.ALL_OCCURRENCES, "Searching in ${file.name}")
        
        val occurrences = arrayListOf<Match>()
        KotlinQueryParticipant().search({ occurrences.add(it) }, querySpecification, NullProgressMonitor())
        
        return occurrences.mapNotNull {
            if (it !is KotlinElementMatch) return@mapNotNull null
            
            val element = it.jetElement
            val length = getLengthOfIdentifier(element) ?: return@mapNotNull null

            val document = editor.getDocumentSafely() ?: return@mapNotNull null
            Position(element.getTextDocumentOffset(document), length)
        }
    }
    
    private fun getSearchingElements(sourceElements: List<SourceElement>): List<SourceElement> {
        val classOrObjects = getContainingClassOrObjectForConstructor(sourceElements)
        return classOrObjects.ifEmpty { sourceElements }
    }
}
