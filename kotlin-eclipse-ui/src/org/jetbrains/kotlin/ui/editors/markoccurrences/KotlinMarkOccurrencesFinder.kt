package org.jetbrains.kotlin.ui.editors.markoccurrences

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
import org.jetbrains.kotlin.ui.editors.withLock

public class KotlinMarkOccurrencesFinder(val editor: KotlinFileEditor) : ISelectionListener {
    private var occurrenceAnnotations = setOf<Annotation>()
    
    override fun selectionChanged(part: IWorkbenchPart, selection: ISelection) {
        if (part is KotlinFileEditor && selection is ITextSelection) {
            val jetElement = EditorUtil.getJetElement(part, selection.getOffset())
            if (jetElement == null) return
            
            val occurrences = findOccurrences(jetElement, part.parsedFile!!)
            updateOccurrences(occurrences)
        }
    }
    
    private fun updateOccurrences(occurrences: List<Position>) {
        val annotationMap = occurrences.toMap { Annotation("org.eclipse.jdt.ui.occurrences", false, "description") }
        val annotationModel = editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput())
        annotationModel.withLock { 
            (annotationModel as IAnnotationModelExtension).replaceAnnotations(occurrenceAnnotations.toTypedArray(), annotationMap)
            occurrenceAnnotations = annotationMap.keySet()
        }
    }
    
    private fun findOccurrences(jetElement: JetElement, jetFile: JetFile): List<Position> {
        val sourceDeclaration = jetElement.resolveToSourceDeclaration(editor.javaProject!!)
        
        val querySpecification = KotlinQuerySpecification(sourceDeclaration, listOf(jetFile), IJavaSearchConstants.ALL_OCCURRENCES)
        
        val occurrences = arrayListOf<Match>()
        KotlinQueryParticipant().search({ occurrences.add(it) }, querySpecification, NullProgressMonitor())
        
        return occurrences.map { 
            val element = (it as KotlinElementMatch).jetElement
            val length = getLengthOfIdentifier(element)!!
            val offset = element.getTextDocumentOffset(editor.document)
            Position(offset, length)
        }
    }
}