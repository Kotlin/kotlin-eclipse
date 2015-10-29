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

class KotlinLightType(val originElement: IType, val editor: KotlinFileEditor) : IType by originElement {
    private val nameRange by lazy {
        object : ISourceRange {
            override fun getLength(): Int = 0
            
            override fun getOffset(): Int = 1
        }
    }
    
    override fun getCompilationUnit(): ICompilationUnit {
        val compilationUnit = EditorUtility.getEditorInputJavaElement(editor, false) as CompilationUnit
        return KotlinLightCompilationUnit(editor.getFile()!!, compilationUnit)
    }
    
    override fun getMethods(): Array<IMethod> = emptyArray()

    override fun getNameRange(): ISourceRange = nameRange

    override fun getPrimaryElement(): IJavaElement? = this

    override fun isBinary(): Boolean = false

    override fun isReadOnly(): Boolean = false
}

class KotlinLightFunction(val originMethod: IMethod, val editor: KotlinFileEditor) : IMethod by originMethod {
    private val nameRange by lazy {
        object : ISourceRange {
            override fun getLength(): Int = 0
            
            override fun getOffset(): Int = 1
        }
    }
    
    override fun getCompilationUnit(): ICompilationUnit {
        val compilationUnit = EditorUtility.getEditorInputJavaElement(editor, false) as CompilationUnit
        return KotlinLightCompilationUnit(editor.getFile()!!, compilationUnit)
    }
    
    override fun getNameRange(): ISourceRange = nameRange

    override fun getPrimaryElement(): IJavaElement? = originMethod

    override fun isBinary(): Boolean = false

    override fun isReadOnly(): Boolean = false
}

class KotlinLightCompilationUnit(val file: IFile, compilationUnit: ICompilationUnit) : ICompilationUnit by compilationUnit,
       ContentProviderCompilationUnit {
    override fun getFileName(): CharArray? = CharOperation.NO_CHAR
    
    override fun getContents(): CharArray? = CharOperation.NO_CHAR
    
    override fun ignoreOptionalProblems(): Boolean = true
    
    override fun getMainTypeName(): CharArray? = CharOperation.NO_CHAR
    
    override fun getPackageName(): Array<out CharArray>? {
        return null
    }

    override fun getResource(): IResource = file
}