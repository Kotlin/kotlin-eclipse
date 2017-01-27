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

package org.jetbrains.kotlin.ui.refactorings.rename

import org.eclipse.jdt.core.IType
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.eclipse.jdt.core.ISourceRange
import org.eclipse.jdt.core.ICompilationUnit
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility
import org.eclipse.jdt.internal.core.CompilationUnit
import org.eclipse.jdt.core.IMethod
import org.eclipse.jdt.core.IJavaElement
import org.eclipse.core.resources.IFile
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit as ContentProviderCompilationUnit
import org.eclipse.jdt.core.compiler.CharOperation
import org.eclipse.core.resources.IResource
import org.jetbrains.kotlin.eclipse.ui.utils.getTextDocumentOffset
import org.eclipse.jdt.internal.ui.JavaPlugin
import org.eclipse.jdt.core.ITypeRoot
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.internal.core.JavaModelManager
import org.eclipse.jdt.internal.core.DefaultWorkingCopyOwner
import org.eclipse.jdt.internal.core.PackageFragment
import org.eclipse.jdt.core.IImportDeclaration
import org.jetbrains.kotlin.ui.navigation.KotlinOpenEditor
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.eclipse.jdt.internal.core.SourceType
import org.eclipse.jdt.internal.core.JavaElement

private val DUMMY_NAME_RANGE = object : ISourceRange {
    override fun getLength(): Int = 0
    
    override fun getOffset(): Int = 1
}

class KotlinLightType(val originElement: IType) : SourceType(originElement.getParent() as JavaElement, originElement.getElementName()) {
    
    override fun findMethods(method: IMethod): Array<out IMethod>? {
        val methods = originElement.findMethods(method)
        return methods
                ?.map { KotlinLightFunction(it) }
                ?.toTypedArray()
    }

    override fun getCompilationUnit(): ICompilationUnit? = getLightCompilationUnit(originElement)
    
    override fun getMethods(): Array<IMethod> = emptyArray()

    override fun getNameRange(): ISourceRange = DUMMY_NAME_RANGE

    override fun getPrimaryElement(): IJavaElement? = this

    override fun isBinary(): Boolean = false

    override fun isReadOnly(): Boolean = false
}

class KotlinLightFunction(val originMethod: IMethod) : IMethod by originMethod {
    override fun getDeclaringType(): IType? {
        val declaringType = originMethod.getDeclaringType()
        return KotlinLightType(declaringType)
    }
    
    override fun getCompilationUnit(): ICompilationUnit? = getLightCompilationUnit(originMethod)
    
    override fun getNameRange(): ISourceRange = DUMMY_NAME_RANGE

    override fun getPrimaryElement(): IJavaElement? = originMethod

    override fun isBinary(): Boolean = false

    override fun isReadOnly(): Boolean = false
}

class KotlinLightCompilationUnit(val file: IFile, compilationUnit: ICompilationUnit) : ICompilationUnit by compilationUnit,
       ContentProviderCompilationUnit {
    override fun getImports(): Array<out IImportDeclaration> = emptyArray()

    override fun getFileName(): CharArray? = CharOperation.NO_CHAR
    
    override fun getContents(): CharArray? = CharOperation.NO_CHAR
    
    override fun ignoreOptionalProblems(): Boolean = true
    
    override fun getMainTypeName(): CharArray? = CharOperation.NO_CHAR
    
    override fun getPackageName(): Array<out CharArray>? = null

    override fun getResource(): IResource = file
}

private fun getLightCompilationUnit(origin: IJavaElement): ICompilationUnit? {
    val sourceFiles = KotlinOpenEditor.findSourceFiles(origin)
    val sourceFile = sourceFiles.firstOrNull()
    if (sourceFile == null) return null
    
    val file = KotlinPsiManager.getEclipseFile(sourceFile)
    if (file == null) return null
    
    val filePackage = JavaModelManager.determineIfOnClasspath(file, origin.getJavaProject())
    return if (filePackage is PackageFragment) {
        val compilationUnit = CompilationUnit(filePackage, file.getName(), DefaultWorkingCopyOwner.PRIMARY)
        KotlinLightCompilationUnit(file, compilationUnit)
    } else null
}