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

import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IResource
import org.eclipse.core.runtime.IPath
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.jobs.ISchedulingRule
import org.eclipse.jdt.core.CompletionRequestor
import org.eclipse.jdt.core.IAnnotation
import org.eclipse.jdt.core.IClassFile
import org.eclipse.jdt.core.ICompilationUnit
import org.eclipse.jdt.core.ICompletionRequestor
import org.eclipse.jdt.core.IField
import org.eclipse.jdt.core.IImportDeclaration
import org.eclipse.jdt.core.IInitializer
import org.eclipse.jdt.core.IJavaElement
import org.eclipse.jdt.core.IJavaModel
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.IMethod
import org.eclipse.jdt.core.IOpenable
import org.eclipse.jdt.core.IPackageFragment
import org.eclipse.jdt.core.ISourceRange
import org.eclipse.jdt.core.IType
import org.eclipse.jdt.core.ITypeHierarchy
import org.eclipse.jdt.core.ITypeParameter
import org.eclipse.jdt.core.ITypeRoot
import org.eclipse.jdt.core.IWorkingCopy
import org.eclipse.jdt.core.WorkingCopyOwner
import org.eclipse.jdt.core.compiler.CharOperation
import org.eclipse.jdt.internal.core.CompilationUnit
import org.eclipse.jdt.internal.core.DefaultWorkingCopyOwner
import org.eclipse.jdt.internal.core.JavaElement
import org.eclipse.jdt.internal.core.JavaModelManager
import org.eclipse.jdt.internal.core.PackageFragment
import org.eclipse.jdt.internal.core.SourceType
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.ui.navigation.KotlinOpenEditor
import java.io.InputStream
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit as ContentProviderCompilationUnit

private val DUMMY_NAME_RANGE = object : ISourceRange {
    override fun getLength(): Int = 0
    
    override fun getOffset(): Int = 1
}

open class Testt(val originElement: IType) : IType by originElement

class KotlinLightType(val originElement: IType) :
        SourceType(originElement.getParent() as JavaElement, originElement.getElementName()),
        IType by originElement {
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
    
    override fun exists(): Boolean = originElement.exists()
    
    override fun <T : Any?> getAdapter(adapter: Class<T>?): T {
        return originElement.getAdapter(adapter)
    }

    override fun isAnnotation(): Boolean = originElement.isAnnotation()

    override fun getOccurrenceCount(): Int = originElement.getOccurrenceCount()

    override fun getInitializers(): Array<out IInitializer>? = originElement.getInitializers()

    override fun getSuperInterfaceTypeSignatures(): Array<out String>? = originElement.getSuperInterfaceTypeSignatures()
    
    override fun getSourceRange(): ISourceRange = originElement.sourceRange

    override fun getSuperclassName(): String? = originElement.getSuperclassName()

    override fun getOpenable(): IOpenable? = originElement.getOpenable()

    override fun getFlags(): Int = originElement.getFlags()

    override fun copy(container: IJavaElement?, sibling: IJavaElement?, rename: String?, replace: Boolean, monitor: IProgressMonitor?) {
        originElement.copy(container, sibling, rename, replace, monitor)
    }

    override fun getSchedulingRule(): ISchedulingRule? = originElement.getSchedulingRule()

    override fun getAttachedJavadoc(monitor: IProgressMonitor?): String? = originElement.getAttachedJavadoc(monitor)

    override fun isInterface(): Boolean = originElement.isInterface()

    override fun getTypeParameters(): Array<out ITypeParameter>? = originElement.getTypeParameters()

    override fun createType(contents: String?, sibling: IJavaElement?, force: Boolean, monitor: IProgressMonitor?): IType? {
        return originElement.createType(contents, sibling, force, monitor)
    }

    override fun createInitializer(contents: String?, sibling: IJavaElement?, monitor: IProgressMonitor?): IInitializer? {
        return originElement.createInitializer(contents, sibling, monitor)
    }

    override fun isAnonymous(): Boolean = originElement.isAnonymous()

    override fun rename(name: String?, replace: Boolean, monitor: IProgressMonitor?) {
        originElement.rename(name, replace, monitor)
    }

    override fun isMember(): Boolean = originElement.isMember()

    override fun getField(name: String?): IField? = originElement.getField(name)

    override fun hashCode(): Int = originElement.hashCode()

    override fun getAnnotations(): Array<out IAnnotation>? = originElement.getAnnotations()

    override fun toString(): String = originElement.toString()

    override fun getTypeRoot(): ITypeRoot? = originElement.getTypeRoot()

    override fun getFullyQualifiedParameterizedName(): String? = originElement.getFullyQualifiedParameterizedName()

    override fun resolveType(typeName: String?): Array<out Array<String>>? = originElement.resolveType(typeName)

    override fun resolveType(typeName: String?, owner: WorkingCopyOwner?): Array<out Array<String>>? {
        return originElement.resolveType(typeName, owner)
    }

    override fun codeComplete(snippet: CharArray?, insertion: Int, position: Int, localVariableTypeNames: Array<out CharArray>?, localVariableNames: Array<out CharArray>?, localVariableModifiers: IntArray?, isStatic: Boolean, requestor: CompletionRequestor?, monitor: IProgressMonitor?) {
        originElement.codeComplete(snippet, insertion, position, localVariableTypeNames, localVariableNames, localVariableModifiers, isStatic, requestor, monitor)
    }

    override fun codeComplete(snippet: CharArray?, insertion: Int, position: Int, localVariableTypeNames: Array<out CharArray>?, localVariableNames: Array<out CharArray>?, localVariableModifiers: IntArray?, isStatic: Boolean, requestor: ICompletionRequestor?) {
        originElement.codeComplete(snippet, insertion, position, localVariableTypeNames, localVariableNames, localVariableModifiers, isStatic, requestor)
    }

    override fun codeComplete(snippet: CharArray?, insertion: Int, position: Int, localVariableTypeNames: Array<out CharArray>?, localVariableNames: Array<out CharArray>?, localVariableModifiers: IntArray?, isStatic: Boolean, requestor: ICompletionRequestor?, owner: WorkingCopyOwner?) {
        originElement.codeComplete(snippet, insertion, position, localVariableTypeNames, localVariableNames, localVariableModifiers, isStatic, requestor, owner)
    }

    override fun codeComplete(snippet: CharArray?, insertion: Int, position: Int, localVariableTypeNames: Array<out CharArray>?, localVariableNames: Array<out CharArray>?, localVariableModifiers: IntArray?, isStatic: Boolean, requestor: CompletionRequestor?) {
        originElement.codeComplete(snippet, insertion, position, localVariableTypeNames, localVariableNames, localVariableModifiers, isStatic, requestor)
    }

    override fun codeComplete(snippet: CharArray?, insertion: Int, position: Int, localVariableTypeNames: Array<out CharArray>?, localVariableNames: Array<out CharArray>?, localVariableModifiers: IntArray?, isStatic: Boolean, requestor: CompletionRequestor?, owner: WorkingCopyOwner?) {
        originElement.codeComplete(snippet, insertion, position, localVariableTypeNames, localVariableNames, localVariableModifiers, isStatic, requestor, owner)
    }

    override fun codeComplete(snippet: CharArray?, insertion: Int, position: Int, localVariableTypeNames: Array<out CharArray>?, localVariableNames: Array<out CharArray>?, localVariableModifiers: IntArray?, isStatic: Boolean, requestor: CompletionRequestor?, owner: WorkingCopyOwner?, monitor: IProgressMonitor?) {
        originElement.codeComplete(snippet, insertion, position, localVariableTypeNames, localVariableNames, localVariableModifiers, isStatic, requestor, owner, monitor)
    }

    override fun getJavadocRange(): ISourceRange? = originElement.getJavadocRange()

    override fun getPath(): IPath? = originElement.getPath()

    override fun loadTypeHierachy(input: InputStream?, monitor: IProgressMonitor?): ITypeHierarchy? {
        return originElement.loadTypeHierachy(input, monitor)
    }

    override fun getCorrespondingResource(): IResource? = originElement.getCorrespondingResource()

    override fun newSupertypeHierarchy(monitor: IProgressMonitor?): ITypeHierarchy? {
        return originElement.newSupertypeHierarchy(monitor)
    }

    override fun newSupertypeHierarchy(workingCopies: Array<out ICompilationUnit>?, monitor: IProgressMonitor?): ITypeHierarchy? {
        return originElement.newSupertypeHierarchy(workingCopies, monitor)
    }

    override fun newSupertypeHierarchy(owner: WorkingCopyOwner?, monitor: IProgressMonitor?): ITypeHierarchy? {
        return originElement.newSupertypeHierarchy(owner, monitor)
    }

    override fun newSupertypeHierarchy(workingCopies: Array<out IWorkingCopy>?, monitor: IProgressMonitor?): ITypeHierarchy? {
        return originElement.newSupertypeHierarchy(workingCopies, monitor)
    }

    override fun getSuperclassTypeSignature(): String? = originElement.getSuperclassTypeSignature()

    override fun isEnum(): Boolean = originElement.isEnum()

    override fun getHandleIdentifier(): String? = originElement.getHandleIdentifier()

    override fun getChildrenForCategory(category: String?): Array<out IJavaElement>? {
        return originElement.getChildrenForCategory(category)
    }

    override fun hasChildren(): Boolean = originElement.hasChildren()

    override fun newTypeHierarchy(monitor: IProgressMonitor?): ITypeHierarchy? {
        return originElement.newTypeHierarchy(monitor)
    }

    override fun newTypeHierarchy(project: IJavaProject?, monitor: IProgressMonitor?): ITypeHierarchy? {
        return originElement.newTypeHierarchy(project, monitor)
    }

    override fun newTypeHierarchy(workingCopies: Array<out IWorkingCopy>?, monitor: IProgressMonitor?): ITypeHierarchy? {
        return originElement.newTypeHierarchy(workingCopies, monitor)
    }

    override fun newTypeHierarchy(owner: WorkingCopyOwner?, monitor: IProgressMonitor?): ITypeHierarchy? {
        return originElement.newTypeHierarchy(owner, monitor)
    }

    override fun newTypeHierarchy(project: IJavaProject?, owner: WorkingCopyOwner?, monitor: IProgressMonitor?): ITypeHierarchy? {
        return originElement.newTypeHierarchy(project, owner, monitor)
    }

    override fun newTypeHierarchy(workingCopies: Array<out ICompilationUnit>?, monitor: IProgressMonitor?): ITypeHierarchy? {
        return originElement.newTypeHierarchy(workingCopies, monitor)
    }

    override fun getResource(): IResource? = originElement.getResource()

    override fun getSource(): String? = originElement.getSource()

    override fun getFields(): Array<out IField>? = originElement.getFields()

    override fun getTypeQualifiedName(): String? = originElement.getTypeQualifiedName()

    override fun getTypeQualifiedName(enclosingTypeSeparator: Char): String? = originElement.getTypeQualifiedName(enclosingTypeSeparator)

    override fun getMethod(name: String?, parameterTypeSignatures: Array<out String>?): IMethod? {
        return originElement.getMethod(name, parameterTypeSignatures)
    }

    override fun isLambda(): Boolean = originElement.isLambda()

    override fun getInitializer(occurrenceCount: Int): IInitializer? = originElement.getInitializer(occurrenceCount)

    override fun getPackageFragment(): IPackageFragment? = originElement.getPackageFragment()

    override fun getElementType(): Int = originElement.getElementType()

    override fun isStructureKnown(): Boolean = originElement.isStructureKnown()

    override fun createField(contents: String?, sibling: IJavaElement?, force: Boolean, monitor: IProgressMonitor?): IField? {
        return originElement.createField(contents, sibling, force, monitor)
    }

    override fun getUnderlyingResource(): IResource? = originElement.getUnderlyingResource()

    override fun getCategories(): Array<out String>? = originElement.getCategories()

    override fun getDeclaringType(): IType? = originElement.getDeclaringType()

    override fun getAncestor(ancestorType: Int): IJavaElement? = originElement.getAncestor(ancestorType)

    override fun delete(force: Boolean, monitor: IProgressMonitor?) {
        originElement.delete(force, monitor)
    }

    override fun getSuperInterfaceNames(): Array<out String>? = originElement.getSuperInterfaceNames()

    override fun getJavaModel(): IJavaModel? = originElement.getJavaModel()

    override fun getParent(): IJavaElement? = originElement.getParent()

    override fun getChildren(): Array<out IJavaElement>? = originElement.getChildren()

    override fun equals(other: Any?): Boolean = originElement.equals(other)

    override fun getTypeParameter(name: String?): ITypeParameter? = originElement.getTypeParameter(name)

    override fun getType(name: String?, occurrenceCount: Int): IType? = originElement.getType(name, occurrenceCount)

    override fun getType(name: String?): IType? = originElement.getType(name)

    override fun getClassFile(): IClassFile? = originElement.getClassFile()

    override fun getTypeParameterSignatures(): Array<out String>? = originElement.getTypeParameterSignatures()

    override fun isClass(): Boolean = originElement.isClass()

    override fun isResolved(): Boolean = originElement.isResolved()

    override fun getKey(): String? = originElement.getKey()

    override fun getAnnotation(name: String?): IAnnotation? = originElement.getAnnotation(name)

    override fun createMethod(contents: String?, sibling: IJavaElement?, force: Boolean, monitor: IProgressMonitor?): IMethod? {
        return originElement.createMethod(contents, sibling, force, monitor)
    }

    override fun getJavaProject(): IJavaProject? = originElement.getJavaProject()

    override fun move(container: IJavaElement?, sibling: IJavaElement?, rename: String?, replace: Boolean, monitor: IProgressMonitor?) {
        originElement.move(container, sibling, rename, replace, monitor)
    }

    override fun getTypes(): Array<out IType>? = originElement.getTypes()

    override fun isLocal(): Boolean = originElement.isLocal()

    override fun getFullyQualifiedName(enclosingTypeSeparator: Char): String? {
        return originElement.getFullyQualifiedName(enclosingTypeSeparator)
    }

    override fun getFullyQualifiedName(): String? = originElement.getFullyQualifiedName()

    override fun getElementName(): String? = originElement.getElementName()
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