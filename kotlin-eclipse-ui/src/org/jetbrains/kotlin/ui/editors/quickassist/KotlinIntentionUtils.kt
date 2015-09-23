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

import org.jetbrains.kotlin.psi.JetNamedDeclaration
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer
import org.eclipse.jdt.core.JavaCore
import org.jetbrains.kotlin.resolve.BindingContextUtils
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.eclipse.core.resources.IProject
import org.jetbrains.kotlin.core.model.KotlinJavaManager
import org.eclipse.jdt.core.IPackageFragmentRoot
import org.eclipse.jdt.core.IMember
import org.eclipse.jdt.core.IMethod
import org.eclipse.jdt.core.IType

fun JetNamedDeclaration.canRemoveTypeSpecificationByVisibility(bindingContext: BindingContext): Boolean {
    val isOverride = getModifierList()?.hasModifier(JetTokens.OVERRIDE_KEYWORD) ?: false
    if (isOverride) return true

    val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, this]
    return descriptor !is DeclarationDescriptorWithVisibility || !descriptor.getVisibility().isPublicAPI
}

fun JetElement.resolveToDescriptor(): DeclarationDescriptor {
    val jetFile = this.getContainingJetFile()
    val project = KotlinPsiManager.getEclispeFile(jetFile)!!.getProject()
    val analysisResult = KotlinAnalyzer.analyzeFile(JavaCore.create(project), jetFile).analysisResult
    return BindingContextUtils.getNotNull(
            analysisResult.bindingContext, 
            BindingContext.DECLARATION_TO_DESCRIPTOR,
            this,
            "Descriptor wasn't found for declaration " + this.toString() + "\n" + this.getElementTextWithContext())
}

fun obtainKotlinPackageFragmentRoots(projects: Array<IProject>): Array<IPackageFragmentRoot> {
    return projects
            .map {
                val javaProject = JavaCore.create(it)
                javaProject.findPackageFragmentRoot(KotlinJavaManager.getKotlinBinFolderFor(it).getFullPath())
            }
            .filterNotNull()
            .toTypedArray()
}

fun unionMembers(types: List<IType>, methods: List<IMethod>): List<IMember> = types + methods