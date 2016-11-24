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
package org.jetbrains.kotlin.ui.editors.completion

import org.eclipse.core.resources.IFile
import org.eclipse.jdt.core.search.SearchPattern
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import com.intellij.openapi.util.text.StringUtilRt
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer
import org.eclipse.jdt.core.JavaCore
import org.jetbrains.kotlin.ui.editors.codeassist.getResolutionScope
import org.jetbrains.kotlin.ui.editors.codeassist.isVisible
import org.eclipse.jdt.ui.PreferenceConstants
import org.eclipse.jdt.internal.ui.JavaPlugin
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.idea.codeInsight.ReferenceVariantsHelper
import org.jetbrains.kotlin.core.model.KotlinEnvironment
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.core.resolve.KotlinResolutionFacade
import org.jetbrains.kotlin.ui.editors.KotlinEditor

public object KotlinCompletionUtils {
    private val KOTLIN_DUMMY_IDENTIFIER = "KotlinRulezzz"
    
    public fun applicableNameFor(prefix: String, name: Name): Boolean {
        return !name.isSpecial && applicableNameFor(prefix, name.identifier)
    }
    
    public fun applicableNameFor(prefix: String, completion: String): Boolean {
        return completion.startsWith(prefix) || 
            completion.toLowerCase().startsWith(prefix) || 
            SearchPattern.camelCaseMatch(prefix, completion)
    }
    
    public fun getReferenceVariants(simpleNameExpression: KtSimpleNameExpression, nameFilter: (Name) -> Boolean, file: IFile): 
            Collection<DeclarationDescriptor> {
        val (analysisResult, container) = KotlinAnalyzer.analyzeFile(simpleNameExpression.getContainingKtFile())
        
        val inDescriptor = simpleNameExpression
                .getReferencedNameElement()
                .getResolutionScope(analysisResult.bindingContext)
                .ownerDescriptor
        
        val showNonVisibleMembers =
            !JavaPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.CODEASSIST_SHOW_VISIBLE_PROPOSALS)
        
        val visibilityFilter = { descriptor: DeclarationDescriptor ->
            when (descriptor) {
                is TypeParameterDescriptor -> descriptor.isVisible(inDescriptor)
                
                is DeclarationDescriptorWithVisibility -> {
                    showNonVisibleMembers || descriptor.isVisible(inDescriptor, analysisResult.bindingContext, simpleNameExpression)
                }
                
                else -> true
            }
        }
        
        return ReferenceVariantsHelper(
                analysisResult.bindingContext,
                KotlinResolutionFacade(file, container, analysisResult.moduleDescriptor),
                analysisResult.moduleDescriptor,
                visibilityFilter).getReferenceVariants(
                simpleNameExpression, DescriptorKindFilter.ALL, nameFilter)
    }
    
    public fun getPsiElement(editor: KotlinEditor, identOffset: Int): PsiElement? {
        val sourceCode = EditorUtil.getSourceCode(editor)
        val sourceCodeWithMarker = StringBuilder(sourceCode).insert(identOffset, KOTLIN_DUMMY_IDENTIFIER).toString()
        val jetFile: KtFile?
        val file = editor.eclipseFile
        if (file != null) {
            jetFile = KotlinPsiManager.parseText(StringUtilRt.convertLineSeparators(sourceCodeWithMarker), file)
        } else {
            KotlinLogger.logError("Failed to retrieve IFile from editor " + editor, null)
            return null
        }
        
        if (jetFile == null) return null
        
        val offsetWithourCR = LineEndUtil.convertCrToDocumentOffset(sourceCodeWithMarker, identOffset, editor.document)
        return jetFile.findElementAt(offsetWithourCR)
    }
    
    public fun replaceMarkerInIdentifier(identifier: String): String {
        return identifier.replaceFirst(KOTLIN_DUMMY_IDENTIFIER, "")
    }
}