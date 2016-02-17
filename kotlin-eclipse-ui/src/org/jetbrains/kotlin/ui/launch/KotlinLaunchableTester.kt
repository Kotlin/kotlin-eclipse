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
package org.jetbrains.kotlin.ui.launch

import org.eclipse.core.expressions.PropertyTester
import org.eclipse.core.resources.IFile
import org.eclipse.core.runtime.IAdaptable
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.JavaCore
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.model.KotlinAnalysisFileCache
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import java.util.ArrayList
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.eclipse.jdt.internal.core.JavaProject

class KotlinLaunchableTester : PropertyTester() {
    override fun test(receiver: Any?, property: String?, args: Array<Any>?, expectedValue: Any?): Boolean {
        if (receiver !is IAdaptable) return false
        
        val file = receiver.getAdapter(IFile::class.java)
        if (file == null) return false
        
        val ktFile = KotlinPsiManager.getKotlinParsedFile(file)
        if (ktFile == null) return false
        
        val javaProject = JavaCore.create(file.getProject())
        return checkFileHashMain(ktFile, javaProject)
    }
}

fun checkFileHashMain(ktFile: KtFile, javaProject: IJavaProject): Boolean {
    val bindingContext = KotlinAnalysisFileCache.getAnalysisResult(ktFile, javaProject).analysisResult.bindingContext
    return MainFunctionDetector(bindingContext).hasMain(obtainFileStaticDeclarations(ktFile))
}

private fun obtainFileStaticDeclarations(ktFile: KtFile): ArrayList<KtDeclaration> {
    val topLevelDeclarations = ktFile.getDeclarations()
    return ArrayList(topLevelDeclarations).apply { 
        for (declaration in topLevelDeclarations) {
            when (declaration) {
                is KtClass -> addAll(declaration.getCompanionObjects().flatMap { it.declarations })
                is KtObjectDeclaration -> addAll(declaration.declarations)
            }
        }
    }
}