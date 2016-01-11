package org.jetbrains.kotlin.ui.editors

import org.eclipse.jdt.core.IClassFile
import org.jetbrains.kotlin.core.resolve.KotlinSourceIndex
import org.jetbrains.kotlin.core.model.KotlinEnvironment
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.core.references.FILE_PROJECT
import org.jetbrains.kotlin.psi.KtFile

fun getSourceFileForAnalysis(ktClassFile: IClassFile): KtFile? {
    val source = KotlinSourceIndex.getSource(ktClassFile)
    if (source != null) {
        val javaProject = ktClassFile.javaProject
        val ideaProject = KotlinEnvironment.getEnvironment(javaProject).project
        val jetFile = KtPsiFactory(ideaProject).createFile(StringUtil.convertLineSeparators(source.toString(),"\n"))
        jetFile.putUserData(FILE_PROJECT, javaProject)
        
        return jetFile
    }
    
    return null
}