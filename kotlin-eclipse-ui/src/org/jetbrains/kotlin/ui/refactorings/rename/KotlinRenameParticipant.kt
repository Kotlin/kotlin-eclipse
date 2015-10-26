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
package org.jetbrains.kotlin.ui.refactorings.rename

import org.eclipse.ltk.core.refactoring.participants.RenameParticipant
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext
import org.eclipse.ltk.core.refactoring.RefactoringStatus
import org.eclipse.ltk.core.refactoring.Change
import kotlin.properties.Delegates
import org.eclipse.jdt.core.IType
import java.util.ArrayList
import org.eclipse.ltk.core.refactoring.CompositeChange
import org.jetbrains.kotlin.ui.search.KotlinQueryParticipant
import org.eclipse.search.ui.text.Match
import org.eclipse.jdt.ui.search.ElementQuerySpecification
import org.eclipse.jdt.core.search.IJavaSearchConstants
import org.eclipse.jdt.internal.ui.search.JavaSearchScopeFactory
import org.jetbrains.kotlin.ui.search.KotlinElementMatch
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.eclipse.ltk.core.refactoring.TextFileChange
import org.jetbrains.kotlin.eclipse.ui.utils.getTextDocumentOffset
import org.eclipse.text.edits.ReplaceEdit
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments
import org.eclipse.ltk.core.refactoring.participants.RenameArguments
import org.eclipse.text.edits.TextEdit
import org.eclipse.core.resources.IFile
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility
import org.jetbrains.kotlin.psi.JetNamedDeclaration
import org.eclipse.jdt.core.IJavaElement
import org.eclipse.jdt.ui.search.QuerySpecification

open class KotlinRenameParticipant : RenameParticipant() {
    lateinit var element: Any
    lateinit var newName: String
    
    override fun initialize(element: Any): Boolean {
        this.element = element
        return true
    }
    
    override fun initialize(processor: RefactoringProcessor?, element: Any?, arguments: RefactoringArguments): Boolean {
        if (arguments is RenameArguments) {
            newName = arguments.getNewName()
        }
        
        return super.initialize(processor, element, arguments)
    }
    
    override fun checkConditions(pm: IProgressMonitor?, context: CheckConditionsContext?): RefactoringStatus? {
        return RefactoringStatus() // TODO: add corresponding refactoring status
    }
    
    override fun getName() = "Kotlin Type Rename Participant"
    
    override fun createChange(pm: IProgressMonitor?): Change {
        val kotlinQueryParticipant = KotlinQueryParticipant()
        val matches = arrayListOf<Match>()
        val querySpecification = createSearchQuery()
        
        kotlinQueryParticipant.search({ matches.add(it) }, querySpecification, pm)
        
        val groupedEdits = matches
            .map { createTextChange(it) }
            .filterNotNull()
            .groupBy { it.file }
        
        val changes = arrayListOf<Change>()
        
        for ((file, edits) in groupedEdits) {
            val fileChange = TextFileChange("Kotlin change", file)
            edits.forEach { TextChangeCompatibility.addTextEdit(fileChange, "Kotlin change", it.edit) }
            
            changes.add(fileChange)
        }
        
        return CompositeChange("Changes in Kotlin", changes.toTypedArray())
    }
    
    protected open fun createSearchQuery(): QuerySpecification {
        val factory = JavaSearchScopeFactory.getInstance()
        return ElementQuerySpecification(
                obtainOriginElement(element as IJavaElement), 
                IJavaSearchConstants.ALL_OCCURRENCES,
                factory.createWorkspaceScope(false),
                factory.getWorkspaceScopeDescription(false))
    }
    
    private fun createTextChange(match: Match): FileEdit? {
        if (match !is KotlinElementMatch) return null
        
        val jetElement = match.jetElement
        
        val eclipseFile = KotlinPsiManager.getEclispeFile(jetElement.getContainingJetFile())
        if (eclipseFile == null) return null
        
        val document = EditorUtil.getDocument(eclipseFile) // TODO: make workaround here later
        
        val textLength = getLengthOfIdentifier(jetElement)
        return if (textLength != null) {
            FileEdit(
                eclipseFile, 
                ReplaceEdit(jetElement.getTextDocumentOffset(document), textLength, newName))
        } else {
            null
        }
    }
    
    private fun obtainOriginElement(javaElement: IJavaElement): IJavaElement {
        return when (javaElement) {
            is KotlinLightType -> javaElement.originElement
            is KotlinLightFunction -> javaElement.originMethod
            else -> javaElement
        }
    }
}

data class FileEdit(val file: IFile, val edit: TextEdit)