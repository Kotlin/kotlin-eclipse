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

import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IMarker
import org.eclipse.jdt.internal.ui.JavaPluginImages
import org.eclipse.swt.graphics.Image
import org.eclipse.ui.IMarkerResolution
import org.eclipse.ui.IMarkerResolution2
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Errors.ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED
import org.jetbrains.kotlin.diagnostics.Errors.ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS
import org.jetbrains.kotlin.diagnostics.Errors.ABSTRACT_FUNCTION_WITH_BODY
import org.jetbrains.kotlin.diagnostics.Errors.ABSTRACT_MEMBER_NOT_IMPLEMENTED
import org.jetbrains.kotlin.diagnostics.Errors.ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS
import org.jetbrains.kotlin.diagnostics.Errors.ABSTRACT_PROPERTY_IN_PRIMARY_CONSTRUCTOR_PARAMETERS
import org.jetbrains.kotlin.diagnostics.Errors.ABSTRACT_PROPERTY_WITH_GETTER
import org.jetbrains.kotlin.diagnostics.Errors.ABSTRACT_PROPERTY_WITH_INITIALIZER
import org.jetbrains.kotlin.diagnostics.Errors.ABSTRACT_PROPERTY_WITH_SETTER
import org.jetbrains.kotlin.diagnostics.Errors.FINAL_SUPERTYPE
import org.jetbrains.kotlin.diagnostics.Errors.FINAL_UPPER_BOUND
import org.jetbrains.kotlin.diagnostics.Errors.GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY
import org.jetbrains.kotlin.diagnostics.Errors.INACCESSIBLE_OUTER_CLASS_EXPRESSION
import org.jetbrains.kotlin.diagnostics.Errors.INCOMPATIBLE_MODIFIERS
import org.jetbrains.kotlin.diagnostics.Errors.INFIX_MODIFIER_REQUIRED
import org.jetbrains.kotlin.diagnostics.Errors.MUST_BE_INITIALIZED_OR_BE_ABSTRACT
import org.jetbrains.kotlin.diagnostics.Errors.NESTED_CLASS_NOT_ALLOWED
import org.jetbrains.kotlin.diagnostics.Errors.NON_ABSTRACT_FUNCTION_WITH_NO_BODY
import org.jetbrains.kotlin.diagnostics.Errors.NON_FINAL_MEMBER_IN_FINAL_CLASS
import org.jetbrains.kotlin.diagnostics.Errors.NON_FINAL_MEMBER_IN_OBJECT
import org.jetbrains.kotlin.diagnostics.Errors.NOTHING_TO_OVERRIDE
import org.jetbrains.kotlin.diagnostics.Errors.OPERATOR_MODIFIER_REQUIRED
import org.jetbrains.kotlin.diagnostics.Errors.OVERRIDING_FINAL_MEMBER
import org.jetbrains.kotlin.diagnostics.Errors.PRIVATE_SETTER_FOR_ABSTRACT_PROPERTY
import org.jetbrains.kotlin.diagnostics.Errors.PRIVATE_SETTER_FOR_OPEN_PROPERTY
import org.jetbrains.kotlin.diagnostics.Errors.REDUNDANT_MODIFIER
import org.jetbrains.kotlin.diagnostics.Errors.REDUNDANT_MODIFIER_FOR_TARGET
import org.jetbrains.kotlin.diagnostics.Errors.REDUNDANT_MODIFIER_IN_GETTER
import org.jetbrains.kotlin.diagnostics.Errors.REPEATED_MODIFIER
import org.jetbrains.kotlin.diagnostics.Errors.SETTER_VISIBILITY_INCONSISTENT_WITH_PROPERTY_VISIBILITY
import org.jetbrains.kotlin.diagnostics.Errors.VIRTUAL_MEMBER_HIDDEN
import org.jetbrains.kotlin.diagnostics.Errors.WRONG_MODIFIER_CONTAINING_DECLARATION
import org.jetbrains.kotlin.diagnostics.Errors.WRONG_MODIFIER_TARGET
import org.jetbrains.kotlin.lexer.KtTokens.ABSTRACT_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.INFIX_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.INNER_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.OPEN_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.OPERATOR_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.OVERRIDE_KEYWORD
import org.jetbrains.kotlin.psi.KtClass

val kotlinQuickFixes = hashMapOf<DiagnosticFactory<*>, MutableList<KotlinQuickFix>>().apply {
    initializeQuickFixes().flatMap { quickFix -> quickFix.handledErrors.map { Pair(it, quickFix) } }
        .groupBy { it.first }
        .forEach { entry ->
            getOrPut(entry.key) { mutableListOf() }.addAll(entry.value.map { it.second })
        }
}

interface KotlinQuickFix {

    val handledErrors: Collection<DiagnosticFactory<*>>

    // this function must be fast and optimistic
    fun canFix(diagnostic: Diagnostic): Boolean = handledErrors.contains(diagnostic.factory)
}

interface KotlinDiagnosticQuickFix : KotlinQuickFix {
    fun getResolutions(diagnostic: Diagnostic): List<KotlinMarkerResolution>
}

interface KotlinMarkerResolution : IMarkerResolution, IMarkerResolution2 {
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

        OVERRIDING_FINAL_MEMBER.createMakeDeclarationOpenFix(),

        MUST_BE_INITIALIZED_OR_BE_ABSTRACT.createAddModifierFix(ABSTRACT_KEYWORD),
        MUST_BE_INITIALIZED_OR_BE_ABSTRACT.createAddModifierFix(ABSTRACT_KEYWORD),
        ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED.createAddModifierFix(ABSTRACT_KEYWORD),
        NON_ABSTRACT_FUNCTION_WITH_NO_BODY.createAddModifierFix(ABSTRACT_KEYWORD),
        ABSTRACT_MEMBER_NOT_IMPLEMENTED.createAddModifierFix(ABSTRACT_KEYWORD),

        ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS.createAddModifierFix(ABSTRACT_KEYWORD, KtClass::class.java),
        ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS.createAddModifierFix(ABSTRACT_KEYWORD, KtClass::class.java),

        VIRTUAL_MEMBER_HIDDEN.createAddModifierFix(OVERRIDE_KEYWORD),

        NON_FINAL_MEMBER_IN_FINAL_CLASS.createAddModifierFix(OPEN_KEYWORD, KtClass::class.java),
        NON_FINAL_MEMBER_IN_OBJECT.createAddModifierFix(OPEN_KEYWORD, KtClass::class.java),

        INACCESSIBLE_OUTER_CLASS_EXPRESSION.createAddModifierFix(INNER_KEYWORD, KtClass::class.java),
        NESTED_CLASS_NOT_ALLOWED.createAddModifierFix(INNER_KEYWORD),

        FINAL_SUPERTYPE.createMakeClassOpenFix(),
        FINAL_UPPER_BOUND.createMakeClassOpenFix(),

        ABSTRACT_PROPERTY_IN_PRIMARY_CONSTRUCTOR_PARAMETERS.createRemoveModifierFromListOwnerFactory(ABSTRACT_KEYWORD),
        ABSTRACT_PROPERTY_WITH_INITIALIZER.createRemoveModifierFromListOwnerFactory(ABSTRACT_KEYWORD),
        ABSTRACT_PROPERTY_WITH_GETTER.createRemoveModifierFromListOwnerFactory(ABSTRACT_KEYWORD),
        ABSTRACT_PROPERTY_WITH_SETTER.createRemoveModifierFromListOwnerFactory(ABSTRACT_KEYWORD),
        ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS.createRemoveModifierFromListOwnerFactory(ABSTRACT_KEYWORD),
        ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS.createRemoveModifierFromListOwnerFactory(ABSTRACT_KEYWORD),
        ABSTRACT_FUNCTION_WITH_BODY.createRemoveModifierFromListOwnerFactory(ABSTRACT_KEYWORD),

        NOTHING_TO_OVERRIDE.createRemoveModifierFromListOwnerFactory(OVERRIDE_KEYWORD),

        NON_FINAL_MEMBER_IN_FINAL_CLASS.createRemoveModifierFromListOwnerFactory(OPEN_KEYWORD),
        NON_FINAL_MEMBER_IN_OBJECT.createRemoveModifierFromListOwnerFactory(OPEN_KEYWORD),

        REDUNDANT_MODIFIER.createRemoveModifierFactory(true),
        REDUNDANT_MODIFIER_IN_GETTER.createRemoveModifierFactory(true),

        INCOMPATIBLE_MODIFIERS.createRemoveModifierFactory(),

        GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY.createRemoveModifierFactory(),
        SETTER_VISIBILITY_INCONSISTENT_WITH_PROPERTY_VISIBILITY.createRemoveModifierFactory(),
        PRIVATE_SETTER_FOR_ABSTRACT_PROPERTY.createRemoveModifierFactory(),
        PRIVATE_SETTER_FOR_OPEN_PROPERTY.createRemoveModifierFactory(),
        REDUNDANT_MODIFIER_IN_GETTER.createRemoveModifierFactory(),
        WRONG_MODIFIER_TARGET.createRemoveModifierFactory(),
        REDUNDANT_MODIFIER_FOR_TARGET.createRemoveModifierFactory(),
        WRONG_MODIFIER_CONTAINING_DECLARATION.createRemoveModifierFactory(),
        REPEATED_MODIFIER.createRemoveModifierFactory(),

        OPERATOR_MODIFIER_REQUIRED.createAddOperatorModifierFix(OPERATOR_KEYWORD),
        INFIX_MODIFIER_REQUIRED.createAddOperatorModifierFix(INFIX_KEYWORD)
    )
}