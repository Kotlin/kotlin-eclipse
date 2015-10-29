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
package org.jetbrains.kotlin.ui.editors.quickassist

import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer
import org.eclipse.jdt.core.JavaCore
import org.jetbrains.kotlin.resolve.BindingContextUtils
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext

fun KtNamedDeclaration.canRemoveTypeSpecificationByVisibility(bindingContext: BindingContext): Boolean {
    val isOverride = getModifierList()?.hasModifier(KtTokens.OVERRIDE_KEYWORD) ?: false
    if (isOverride) return true

    val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, this]
    return descriptor !is DeclarationDescriptorWithVisibility || !descriptor.getVisibility().isPublicAPI
}

fun KtElement.resolveToDescriptor(): DeclarationDescriptor {
    val jetFile = this.getContainingJetFile()
    val project = KotlinPsiManager.getEclispeFile(jetFile)!!.getProject()
    val analysisResult = KotlinAnalyzer.analyzeFile(JavaCore.create(project), jetFile).analysisResult
    return BindingContextUtils.getNotNull(
            analysisResult.bindingContext, 
            BindingContext.DECLARATION_TO_DESCRIPTOR,
            this,
            "Descriptor wasn't found for declaration " + this.toString() + "\n" + this.getElementTextWithContext())
}