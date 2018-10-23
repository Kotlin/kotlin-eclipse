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

import com.intellij.psi.util.PsiTreeUtil
import org.eclipse.core.expressions.PropertyTester
import org.eclipse.core.resources.IFile
import org.eclipse.core.runtime.IAdaptable
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.model.KotlinAnalysisFileCache
import org.jetbrains.kotlin.core.model.KotlinEnvironment
import org.jetbrains.kotlin.core.preferences.languageVersionSettings
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*

class KotlinLaunchableTester : PropertyTester() {
    override fun test(receiver: Any?, property: String?, args: Array<Any>?, expectedValue: Any?): Boolean {
        if (receiver !is IAdaptable) return false

        val file = receiver.getAdapter(IFile::class.java) ?: return false

        val ktFile = KotlinPsiManager.getKotlinParsedFile(file) ?: return false

        return checkFileHasMain(
                ktFile,
                KotlinEnvironment.getEnvironment(file.project).compilerProperties.languageVersionSettings)
    }
}

fun checkFileHasMain(ktFile: KtFile, languageVersion: LanguageVersionSettings): Boolean {
    return getEntryPoint(ktFile, languageVersion) != null
}

fun getStartClassFqName(mainFunctionDeclaration: KtDeclaration): FqName? {
    val container = mainFunctionDeclaration.declarationContainer()
    return when (container) {
        is KtFile -> JvmFileClassUtil.getFileClassInfoNoResolve(container).facadeClassFqName

        is KtClassOrObject -> {
            if (container is KtObjectDeclaration && container.isCompanion()) {
                val containerClass = PsiTreeUtil.getParentOfType(container, KtClass::class.java)
                containerClass?.fqName
            } else {
                container.fqName
            }
        }

        else -> null
    }
}

fun getEntryPoint(ktFile: KtFile, languageVersion: LanguageVersionSettings): KtDeclaration? {
    val bindingContext = KotlinAnalysisFileCache.getAnalysisResult(ktFile).analysisResult.bindingContext
    val mainFunctionDetector = MainFunctionDetector(bindingContext, languageVersion)

    val topLevelDeclarations = ktFile.declarations
    for (declaration in topLevelDeclarations) {
        val mainFunction = when (declaration) {
            is KtNamedFunction -> if (mainFunctionDetector.isMain(declaration)) declaration else null

            is KtClass -> {
                mainFunctionDetector.findMainFunction(declaration.companionObjects.flatMap { it.declarations })
            }

            is KtObjectDeclaration -> mainFunctionDetector.findMainFunction(declaration.declarations)

            else -> null
        }

        if (mainFunction != null) return mainFunction
    }

    return null
}

private fun KtDeclaration.declarationContainer(): KtDeclarationContainer? {
    return PsiTreeUtil.getParentOfType(this, KtClassOrObject::class.java, KtFile::class.java) as KtDeclarationContainer?
}

private fun MainFunctionDetector.findMainFunction(declarations: List<KtDeclaration>): KtNamedFunction? {
    return declarations.filterIsInstance(KtNamedFunction::class.java).find { isMain(it) }
}