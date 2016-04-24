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

package org.jetbrains.kotlin.eclipse.ui.utils

import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer
import org.jetbrains.kotlin.psi.KtFile
import org.eclipse.jdt.core.IJavaProject
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.ui.editors.KotlinEditor
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.core.builder.KotlinPsiManager

fun getBindingContext(kotlinEditor: KotlinEditor): BindingContext? {
    val ktFile = kotlinEditor.parsedFile
    val javaProject = kotlinEditor.javaProject
    return if (ktFile != null && javaProject != null) getBindingContext(ktFile, javaProject) else null
}

fun getBindingContext(ktElement: KtElement): BindingContext? {
    val javaProject = KotlinPsiManager.getJavaProject(ktElement)
    return if (javaProject != null) getBindingContext(ktElement.getContainingKtFile(), javaProject) else null
}

fun getBindingContext(ktFile: KtFile, javaProject: IJavaProject): BindingContext? {
    return KotlinAnalyzer.analyzeFile(javaProject, ktFile).analysisResult.bindingContext
}