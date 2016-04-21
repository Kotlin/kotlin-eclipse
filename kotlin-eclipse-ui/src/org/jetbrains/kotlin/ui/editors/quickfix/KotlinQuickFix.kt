/*******************************************************************************
 * Copyright 2000-2016 JetBrains s.r.o.
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
package org.jetbrains.kotlin.ui.editors.quickfix

import org.eclipse.core.resources.IMarker
import org.jetbrains.kotlin.ui.editors.annotations.AnnotationManager.MARKER_PROBLEM_TYPE
import org.jetbrains.kotlin.ui.editors.annotations.AnnotationManager.CAN_FIX_PROBLEM
import org.eclipse.ui.IMarkerResolution2
import org.eclipse.ui.IMarkerResolution
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.eclipse.core.resources.IFile
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.ui.editors.quickassist.getBindingContext
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jface.text.IDocument
import org.jetbrains.kotlin.eclipse.ui.utils.findElementByDocumentOffset
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.eclipse.swt.graphics.Image
import org.eclipse.jdt.internal.ui.JavaPluginImages
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.psi.KtClass

val kotlinQuickFixes: List<KotlinQuickFix> = initializeQuickFixes()

interface KotlinQuickFix {
    // this function must be fast and optimistic
    fun canFix(diagnostic: Diagnostic): Boolean
}

interface KotlinDiagnosticQuickFix : KotlinQuickFix {
    fun getResolutions(diagnostic: Diagnostic): List<KotlinMarkerResolution>
}

interface KotlinMarkerResolution: IMarkerResolution, IMarkerResolution2 {
    fun apply(file: IFile)
    
    override fun run(marker: IMarker) {
        val resource = marker.resource
        if (resource is IFile) {
            apply(resource)
        }
    }
    
    override fun getImage(): Image? = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE)
    
    override fun getDescription(): String? = null
}

private fun initializeQuickFixes(): List<KotlinQuickFix> {
    return listOf(
            KotlinAutoImportQuickFix,
            
            MUST_BE_INITIALIZED_OR_BE_ABSTRACT.createAddModifierFix(ABSTRACT_KEYWORD),
            MUST_BE_INITIALIZED_OR_BE_ABSTRACT.createAddModifierFix(ABSTRACT_KEYWORD),
            ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED.createAddModifierFix(ABSTRACT_KEYWORD),
            NON_ABSTRACT_FUNCTION_WITH_NO_BODY.createAddModifierFix(ABSTRACT_KEYWORD),
            
            ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS.createAddModifierFix(ABSTRACT_KEYWORD, KtClass::class.java),
            ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS.createAddModifierFix(ABSTRACT_KEYWORD, KtClass::class.java),
    
            VIRTUAL_MEMBER_HIDDEN.createAddModifierFix(OVERRIDE_KEYWORD),
            
            NON_FINAL_MEMBER_IN_FINAL_CLASS.createAddModifierFix(OPEN_KEYWORD, KtClass::class.java),
            NON_FINAL_MEMBER_IN_OBJECT.createAddModifierFix(OPEN_KEYWORD, KtClass::class.java),
    
            INACCESSIBLE_OUTER_CLASS_EXPRESSION.createAddModifierFix(INNER_KEYWORD, KtClass::class.java),
            NESTED_CLASS_NOT_ALLOWED.createAddModifierFix(INNER_KEYWORD))
}