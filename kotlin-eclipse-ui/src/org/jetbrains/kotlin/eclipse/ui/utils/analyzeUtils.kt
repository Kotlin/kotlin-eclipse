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
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.ui.editors.KotlinEditor

fun getBindingContext(kotlinEditor: KotlinEditor): BindingContext? =
    kotlinEditor.parsedFile?.let { getBindingContext(it) }

fun getBindingContext(ktElement: KtElement): BindingContext? =
    getBindingContext(ktElement.containingKtFile)

fun getBindingContext(ktFile: KtFile): BindingContext? =
    KotlinAnalyzer.analyzeFile(ktFile).analysisResult.bindingContext

fun getModuleDescriptor(ktFile: KtFile): ModuleDescriptor =
    KotlinAnalyzer.analyzeFile(ktFile).analysisResult.moduleDescriptor