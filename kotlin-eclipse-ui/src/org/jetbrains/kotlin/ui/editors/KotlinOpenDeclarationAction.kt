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
package org.jetbrains.kotlin.ui.editors

import java.lang.reflect.Field
import java.lang.reflect.Method
import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.runtime.Path
import org.eclipse.jdt.core.IClassFile
import org.eclipse.jdt.core.IJavaElement
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.IPackageFragment
import org.eclipse.jdt.core.IType
import org.eclipse.jdt.core.JavaModelException
import org.eclipse.jdt.core.dom.IBinding
import org.eclipse.jdt.core.dom.IMethodBinding
import org.eclipse.jdt.internal.ui.actions.ActionMessages
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility
import org.eclipse.jdt.ui.JavaUI
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds
import org.eclipse.jdt.ui.actions.SelectionDispatchAction
import org.eclipse.jface.text.ITextSelection
import org.eclipse.jface.util.OpenStrategy
import org.eclipse.ui.IEditorPart
import org.eclipse.ui.IReusableEditor
import org.eclipse.ui.IStorageEditorInput
import org.eclipse.ui.IWorkbench
import org.eclipse.ui.IWorkbenchPage
import org.eclipse.ui.IWorkbenchWindow
import org.eclipse.ui.PartInitException
import org.eclipse.ui.PlatformUI
import org.eclipse.ui.ide.IDE
import org.eclipse.ui.texteditor.AbstractTextEditor
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.core.references.KotlinReference
import org.jetbrains.kotlin.core.references.createReferences
import org.jetbrains.kotlin.core.references.resolveToSourceElements
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer
import org.jetbrains.kotlin.core.resolve.lang.java.resolver.EclipseJavaSourceElement
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryPackageSourceElement
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlin.load.kotlin.VirtualFileKotlinClass
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader.Kind
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor
import org.jetbrains.kotlin.ui.navigation.KotlinOpenEditor
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.eclipse.ui.utils.getTextDocumentOffset
import org.jetbrains.kotlin.core.references.getReferenceExpression

class KotlinOpenDeclarationAction(val editor: KotlinEditor) : SelectionDispatchAction(editor.javaEditor.site) {
    companion object {
        const val OPEN_EDITOR_TEXT = "OpenEditor"
    }
    
    init {
        setText(ActionMessages.OpenAction_declaration_label)
        setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_EDITOR)
    }
    
    override fun run(selection: ITextSelection) {
        val selectedExpression = getSelectedExpressionWithParsedFile(editor, selection.offset)
        if (selectedExpression == null) return
        
        val references = createReferences(selectedExpression)
        val element = getTargetElement(references)
        if (element == null) return
        
        val javaProject = editor.javaProject
        if (javaProject == null) return
        
        gotoElement(element, references.first(), javaProject)
    }
    
    private fun getTargetElement(reference: List<KotlinReference>): SourceElement? {
        return reference.resolveToSourceElements().find { it != SourceElement.NO_SOURCE }
    }
    
    private fun gotoElement(element: SourceElement, kotlinReference: KotlinReference, javaProject: IJavaProject) {
        when (element) {
            is EclipseJavaSourceElement -> {
                val binding = (element.javaElement as EclipseJavaElement<*>).getBinding()
                gotoJavaDeclaration(binding)
            }
            
            is KotlinSourceElement -> gotoKotlinDeclaration(element.psi, kotlinReference, javaProject)
            
            is KotlinJvmBinarySourceElement -> {
                val binaryClass = element.binaryClass
                val descriptor = getDeclarationDescriptor(kotlinReference, javaProject)
                if (descriptor == null) {
                    KotlinLogger.logWarning("Declaration descriptor for $kotlinReference is null")
                    return
                }
                
                gotoElementInBinaryClass(binaryClass, descriptor, javaProject)
            }
            
            is KotlinJvmBinaryPackageSourceElement -> gotoClassByPackageSourceElement(element, kotlinReference, javaProject)
        }
    }
    
    private fun gotoClassByPackageSourceElement(
            sourceElement: KotlinJvmBinaryPackageSourceElement, 
            kotlinReference: KotlinReference, 
            javaProject: IJavaProject) {
        val descriptor = getDeclarationDescriptor(kotlinReference, javaProject)
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
    
    private fun getDeclarationDescriptor(kotlinReference: KotlinReference, javaProject: IJavaProject): DeclarationDescriptor? {
        val jetFile = kotlinReference.expression.getContainingKtFile()
        val context = KotlinAnalyzer.analyzeFile(javaProject, jetFile).analysisResult.bindingContext
        
        return kotlinReference.getTargetDescriptors(context).firstOrNull() //TODO: popup if there's several descriptors to navigate to
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
    
    private fun getClassFile(binaryClass: KotlinJvmBinaryClass, javaProject: IJavaProject): IClassFile? {
        val file = (binaryClass as VirtualFileKotlinClass).file
        val fragment = javaProject.findPackageFragment(pathFromUrlInArchive(file.parent.path))
        return fragment?.getClassFile(file.name)
    }
    
    private fun gotoKotlinDeclaration(element: PsiElement, kotlinReference: KotlinReference, javaProject: IJavaProject) {
        val targetEditor = findEditorForReferencedElement(element, kotlinReference, javaProject)
        if (targetEditor !is KotlinEditor) return
        
        val start = element.getTextDocumentOffset(targetEditor.document)
        targetEditor.selectAndReveal(start, 0)
    }
    
    private fun findEditorForReferencedElement(
            element: PsiElement,
            kotlinReference: KotlinReference, 
            javaProject: IJavaProject): AbstractTextEditor? {
        // if element is in the same file
        if (kotlinReference.expression.getContainingFile() == element.getContainingFile()) {
            return editor.javaEditor
        }
        
        val virtualFile = element.getContainingFile().getVirtualFile()
        if (virtualFile == null) return null
        
        val filePath = virtualFile.path
        val targetFile = ResourcesPlugin.getWorkspace().root.getFileForLocation(Path(filePath)) ?: getAcrhivedFileFromPath(filePath)
        
        return findEditorPart(targetFile, element, javaProject) as? AbstractTextEditor
    }
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

fun getSelectedExpression(editor: KotlinEditor, offset: Int) = getSelectedExpressionWithParsedFile(editor, offset)

fun getSelectedExpressionWithParsedFile(editor: KotlinEditor, offset: Int): KtReferenceExpression? {
    val file = editor.parsedFile
    if (file == null) return null
    
    val documentOffset = LineEndUtil.convertCrToDocumentOffset(editor.document, offset)
    val psiExpression = file.findElementAt(documentOffset)
    if (psiExpression == null) return null
    
    return getReferenceExpression(psiExpression)
}

fun openInEditor(file: IFile): IEditorPart {
    val page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
    return IDE.openEditor(page, file, false)
}