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
package org.jetbrains.kotlin.ui.editors.navigation

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.runtime.Path
import org.eclipse.jdt.core.IClassFile
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.IPackageFragment
import org.eclipse.jdt.core.dom.IBinding
import org.eclipse.jdt.core.dom.IMethodBinding
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility
import org.eclipse.jdt.ui.JavaUI
import org.eclipse.jface.util.OpenStrategy
import org.eclipse.ui.IEditorPart
import org.eclipse.ui.IReusableEditor
import org.eclipse.ui.PlatformUI
import org.eclipse.ui.ide.IDE
import org.eclipse.ui.texteditor.AbstractTextEditor
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.core.resolve.EclipseDescriptorUtils
import org.jetbrains.kotlin.core.resolve.lang.java.resolver.EclipseJavaSourceElement
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil
import org.jetbrains.kotlin.eclipse.ui.utils.getTextDocumentOffset
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryPackageSourceElement
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlin.load.kotlin.VirtualFileKotlinClass
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor
import org.jetbrains.kotlin.ui.editors.KotlinClassFileEditor
import org.jetbrains.kotlin.ui.editors.KotlinEditor
import org.jetbrains.kotlin.ui.navigation.KotlinOpenEditor

fun gotoElement(descriptor: DeclarationDescriptor, fromElement: KtElement,
                fromEditor: KotlinEditor, javaProject: IJavaProject) {
    val elementWithSource = getElementWithSource(descriptor, javaProject.project)
    if (elementWithSource != null) gotoElement(elementWithSource, descriptor, fromElement, fromEditor, javaProject)
}

fun getElementWithSource(descriptor: DeclarationDescriptor, project: IProject): SourceElement? {
    val sourceElements = EclipseDescriptorUtils.descriptorToDeclarations(descriptor, project)
    return sourceElements.find { element -> element != SourceElement.NO_SOURCE }
}

fun gotoElement(
        element: SourceElement, 
        descriptor: DeclarationDescriptor, 
        fromElement: KtElement, 
        fromEditor: KotlinEditor, 
        project: IJavaProject) {
    when (element) {
        is EclipseJavaSourceElement -> {
            val binding = (element.javaElement as EclipseJavaElement<*>).getBinding()
            gotoJavaDeclaration(binding)
        }
        
        is KotlinSourceElement -> gotoKotlinDeclaration(element.psi, fromElement, fromEditor, project)
        
        is KotlinJvmBinarySourceElement -> gotoElementInBinaryClass(element.binaryClass, descriptor, project)
        
        is KotlinJvmBinaryPackageSourceElement -> gotoClassByPackageSourceElement(element, descriptor, project)
    }
}

private fun getClassFile(binaryClass: KotlinJvmBinaryClass, javaProject: IJavaProject): IClassFile? {
    val file = (binaryClass as VirtualFileKotlinClass).file
    val fragment = javaProject.findPackageFragment(pathFromUrlInArchive(file.parent.path))
    return fragment?.getClassFile(file.name)
}

private fun findImplementingClass(
            binaryClass: KotlinJvmBinaryClass, 
            descriptor: DeclarationDescriptor, 
            javaProject: IJavaProject): IClassFile? {
        return if (KotlinClassHeader.Kind.MULTIFILE_CLASS == binaryClass.classHeader.kind) 
            getImplementingFacadePart(binaryClass, descriptor, javaProject)
        else 
            getClassFile(binaryClass, javaProject)
    }
    
private fun getImplementingFacadePart(
        binaryClass: KotlinJvmBinaryClass, 
        descriptor: DeclarationDescriptor,
        javaProject: IJavaProject): IClassFile? {
    if (descriptor !is DeserializedCallableMemberDescriptor) return null
    
    val className = getImplClassName(descriptor)
    if (className == null) {
        KotlinLogger.logWarning("Class file with implementation of $descriptor is null")
        return null
    }
    
    val classFile = getClassFile(binaryClass, javaProject)
    if (classFile == null) {
        KotlinLogger.logWarning("Class file for $binaryClass is null")
        return null
    }
    
    val fragment = classFile.getParent() as IPackageFragment
    val file = fragment.getClassFile(className.asString() + ".class")
    
    return if (file.exists()) file else null
}

//implemented via Reflection because Eclipse has issues with ProGuard-shrunken compiler
//in com.google.protobuf.GeneratedMessageLite.ExtendableMessage
private fun getImplClassName(memberDescriptor: DeserializedCallableMemberDescriptor): Name? {
    val nameIndex: Int
    
    try
    {
        val getProtoMethod = DeserializedCallableMemberDescriptor::class.java.getMethod("getProto")
        val proto = getProtoMethod!!.invoke(memberDescriptor)
        val implClassNameField = Class.forName("org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf").getField("implClassName")
        val implClassName = implClassNameField!!.get(null)
        val protobufCallable = Class.forName("org.jetbrains.kotlin.serialization.ProtoBuf\$Callable")
        val getExtensionMethod = protobufCallable!!.getMethod("getExtension", implClassName!!.javaClass)
        val indexObj = getExtensionMethod!!.invoke(proto, implClassName)
    
        if (indexObj !is Int) return null
    
        nameIndex = indexObj.toInt()
    } catch (e: ReflectiveOperationException) {
        KotlinLogger.logAndThrow(e)
        return null
    } catch (e: IllegalArgumentException) {
        KotlinLogger.logAndThrow(e)
        return null
    } catch (e: SecurityException) {
        KotlinLogger.logAndThrow(e)
        return null
    }
    
    return memberDescriptor.nameResolver.getName(nameIndex)
}

private fun gotoClassByPackageSourceElement(
        sourceElement: KotlinJvmBinaryPackageSourceElement, 
        descriptor: DeclarationDescriptor, 
        javaProject: IJavaProject) {
    if (descriptor !is DeserializedCallableMemberDescriptor) return 
    
    val binaryClass = sourceElement.getContainingBinaryClass(descriptor)
    if (binaryClass == null) {
        KotlinLogger.logWarning("Containing binary class for $sourceElement is null")
        return
    }
    
    gotoElementInBinaryClass(binaryClass, descriptor, javaProject)
}

private fun gotoElementInBinaryClass(
        binaryClass: KotlinJvmBinaryClass, 
        descriptor: DeclarationDescriptor,
        javaProject: IJavaProject) {
    val classFile = findImplementingClass(binaryClass, descriptor, javaProject)
    if (classFile == null) return
    
    val targetEditor = KotlinOpenEditor.openKotlinClassFileEditor(classFile, OpenStrategy.activateOnOpen())
    if (targetEditor !is KotlinClassFileEditor) return
    
    val targetKtFile = targetEditor.parsedFile
    val offset = findDeclarationInParsedFile(descriptor, targetKtFile)
    val start = LineEndUtil.convertLfToDocumentOffset(targetKtFile.getText(), offset, targetEditor.document)
    
    targetEditor.selectAndReveal(start, 0)
}

private fun gotoKotlinDeclaration(element: PsiElement, fromElement: KtElement, fromEditor: KotlinEditor, javaProject: IJavaProject) {
    val targetEditor = findEditorForReferencedElement(element, fromElement, fromEditor, javaProject)
    if (targetEditor !is KotlinEditor) return
    
    val start = element.getTextDocumentOffset(targetEditor.document)
    targetEditor.selectAndReveal(start, 0)
}

private fun findEditorForReferencedElement(
        element: PsiElement,
        fromElement: KtElement, 
        fromEditor: KotlinEditor,
        javaProject: IJavaProject): AbstractTextEditor? {
    // if element is in the same file
    if (fromElement.getContainingFile() == element.getContainingFile()) {
        return fromEditor.javaEditor
    }
    
    val virtualFile = element.getContainingFile().getVirtualFile()
    if (virtualFile == null) return null
    
    val filePath = virtualFile.path
    val targetFile = ResourcesPlugin.getWorkspace().root.getFileForLocation(Path(filePath)) ?: getAcrhivedFileFromPath(filePath)
    
    return findEditorPart(targetFile, element, javaProject) as? AbstractTextEditor
}

private fun gotoJavaDeclaration(binding: IBinding) {
    val javaElement = if (binding is IMethodBinding && binding.isConstructor()) 
        binding.getDeclaringClass().getJavaElement() // because <init>() may correspond to null java element
     else
        binding.javaElement
    
    if (javaElement != null) {
        val editorPart = EditorUtility.openInEditor(javaElement, OpenStrategy.activateOnOpen())
        JavaUI.revealInEditor(editorPart, javaElement)
    }
}

private fun findEditorPart(
        targetFile: IFile, 
        element: PsiElement,
        javaProject: IJavaProject): IEditorPart? {
    if (targetFile.exists()) return openInEditor(targetFile)
    
    val psiClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java)
    if (psiClass != null) {
        val targetType = javaProject.findType(psiClass.getQualifiedName())
        return EditorUtility.openInEditor(targetType, true)
    }
    
    //external jar
    if (targetFile.getFullPath().toOSString().contains("jar")) {
        val page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
        if (page == null) return null
        
        val elementFile = element.getContainingFile()
        if (elementFile == null) return null
        
        val directory = elementFile.getContainingDirectory()
        val storage = StringStorage(elementFile.text, elementFile.name, getFqNameInsideArchive(directory.toString()))
        val input = StringInput(storage)
        val reusedEditor = page.findEditor(input)
        if (reusedEditor != null) {
            page.reuseEditor(reusedEditor as IReusableEditor?, input)
        }
        
        return page.openEditor(input, "org.jetbrains.kotlin.ui.editors.KotlinFileEditor")
    }
    
    return null
}

private fun openInEditor(file: IFile): IEditorPart {
    val page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
    return IDE.openEditor(page, file, false)
}