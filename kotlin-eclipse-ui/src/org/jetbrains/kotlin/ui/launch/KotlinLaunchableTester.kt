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

class KotlinLaunchableTester : PropertyTester() {
    override fun test(receiver: Any?, property: String?, args: Array<Any>?, expectedValue: Any?): Boolean {
        if (receiver !is IAdaptable) return false
        
        val file = receiver.getAdapter(IFile::class.java)
        if (file == null) return false
        
        val jetFile = KotlinPsiManager.getKotlinParsedFile(file)
        if (jetFile == null) return false
        
        val javaProject = JavaCore.create(file.getProject())
        val bindingContext = KotlinAnalysisFileCache.getAnalysisResult(jetFile, javaProject).analysisResult.bindingContext
        return MainFunctionDetector(bindingContext).hasMain(jetFile.getDeclarations())
    }
}