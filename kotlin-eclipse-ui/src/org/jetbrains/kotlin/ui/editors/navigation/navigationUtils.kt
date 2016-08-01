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

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.runtime.IPath
import org.eclipse.core.runtime.Path
import org.eclipse.jdt.core.IClassFile
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.IPackageFragment
import org.eclipse.jdt.core.dom.IBinding
import org.eclipse.jdt.core.dom.IMethodBinding
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader
import org.eclipse.jdt.internal.core.SourceMapper
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility
import org.eclipse.jdt.ui.JavaUI
import org.eclipse.jface.util.OpenStrategy
import org.eclipse.ui.IEditorPart
import org.eclipse.ui.IReusableEditor
import org.eclipse.ui.IStorageEditorInput
import org.eclipse.ui.PlatformUI
import org.eclipse.ui.ide.IDE
import org.eclipse.ui.texteditor.AbstractTextEditor
import org.jetbrains.kotlin.core.KotlinClasspathContainer
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.core.model.KotlinNature
import org.jetbrains.kotlin.core.resolve.EclipseDescriptorUtils
import org.jetbrains.kotlin.core.resolve.KotlinSourceIndex
import org.jetbrains.kotlin.core.resolve.lang.java.resolver.EclipseJavaSourceElement
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaElement
import org.jetbrains.kotlin.core.utils.ProjectUtils
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil
import org.jetbrains.kotlin.eclipse.ui.utils.getTextDocumentOffset
import org.jetbrains.kotlin.load.java.sources.JavaSourceElement
import org.jetbrains.kotlin.load.java.structure.JavaElement
import org.jetbrains.kotlin.load.java.structure.impl.JavaElementImpl
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryPackageSourceElement
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlin.load.kotlin.VirtualFileKotlinClass
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.resolve.source.PsiSourceElement
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor
import org.jetbrains.kotlin.ui.editors.KotlinEditor
import org.jetbrains.kotlin.ui.editors.KotlinExternalReadOnlyEditor
import org.jetbrains.kotlin.ui.editors.KotlinScriptEditor
import org.jetbrains.kotlin.ui.editors.getScriptDependencies
import org.jetbrains.kotlin.ui.formatter.createKtFile
import org.jetbrains.kotlin.ui.navigation.KotlinOpenEditor

private val KOTLIN_SOURCE_PATH = Path(ProjectUtils.buildLibPath(KotlinClasspathContainer.LIB_RUNTIME_SRC_NAME))
private val RUNTIME_SOURCE_MAPPER = KOTLIN_SOURCE_PATH.createSourceMapperWithRoot()

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
        
        is KotlinJvmBinarySourceElement -> gotoElementInBinaryClass(element.binaryClass, descriptor, fromElement, project)
        
        is KotlinJvmBinaryPackageSourceElement -> gotoClassByPackageSourceElement(element, fromElement, descriptor, project)
        
        is PsiSourceElement -> {
            if (element is JavaSourceElement) {
                gotoJavaDeclarationFromNonClassPath(element.javaElement, element.psi, fromEditor, project)
            }
        }
    }
}

private fun gotoJavaDeclarationFromNonClassPath(
        javaElement: JavaElement,
        psi: PsiElement?,
        fromEditor: KotlinEditor,
        javaProject: IJavaProject) {
    if (!fromEditor.isScript) return
    
    val javaPsi = (javaElement as JavaElementImpl<*>).psi
    
    val editorPart = tryToFindSourceInJavaProject(javaPsi, javaProject)
    if (editorPart != null) {
        revealJavaElementInEditor(editorPart, javaElement, EditorUtil.getSourceCode(editorPart))
        return
    }
    
    val virtualFile = psi?.containingFile?.virtualFile ?: return
    val (sourceName, packagePath) = findSourceFilePath(virtualFile)
    
    val dependencies = getScriptDependencies(fromEditor as KotlinScriptEditor) ?: return
    
    val pathToSource = packagePath.append(sourceName)

    val source = dependencies.sources.asSequence()
            .map { Path(it.absolutePath).createSourceMapperWithRoot() }
            .mapNotNull { it.findSource(pathToSource.toOSString()) }
            .firstOrNull() ?: return
    
    val sourceString = String(source)
    val targetEditor = openJavaEditorForExternalFile(sourceString, sourceName, packagePath.toOSString()) ?: return
    
    revealJavaElementInEditor(targetEditor, javaElement, sourceString)
    
    return
}

private fun revealJavaElementInEditor(editor: IEditorPart, javaElement: JavaElement, source: String) {
    val offset = findDeclarationInJavaFile(javaElement, source)
    if (offset != null && editor is AbstractTextEditor) {
        editor.selectAndReveal(offset, 0)
    }
}

private fun getClassFile(binaryClass: VirtualFileKotlinClass, javaProject: IJavaProject): IClassFile? {
    val file = binaryClass.file
    val fragment = javaProject.findPackageFragment(pathFromUrlInArchive(file.parent.path))
    return fragment?.getClassFile(file.name)
}

private fun findImplementingClassInClasspath(
            binaryClass: VirtualFileKotlinClass, 
            descriptor: DeclarationDescriptor, 
            javaProject: IJavaProject): IClassFile? {
        return if (KotlinClassHeader.Kind.MULTIFILE_CLASS == binaryClass.classHeader.kind) 
            getImplementingFacadePart(binaryClass, descriptor, javaProject)
        else 
            getClassFile(binaryClass, javaProject)
    }
    
private fun getImplementingFacadePart(
        binaryClass: VirtualFileKotlinClass, 
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
        fromElement: KtElement,
        descriptor: DeclarationDescriptor, 
        javaProject: IJavaProject) {
    if (descriptor !is DeserializedCallableMemberDescriptor) return 
    
    val binaryClass = sourceElement.getContainingBinaryClass(descriptor)
    if (binaryClass == null) {
        KotlinLogger.logWarning("Containing binary class for $sourceElement is null")
        return
    }
    
    gotoElementInBinaryClass(binaryClass, descriptor, fromElement, javaProject)
}

private fun gotoElementInBinaryClass(
        binaryClass: KotlinJvmBinaryClass, 
        descriptor: DeclarationDescriptor,
        fromElement: KtElement,
        javaProject: IJavaProject) {
    if (binaryClass !is VirtualFileKotlinClass) return
    
    val classFile = findImplementingClassInClasspath(binaryClass, descriptor, javaProject)

    val targetEditor = if (classFile != null) {
        KotlinOpenEditor.openKotlinClassFileEditor(classFile, OpenStrategy.activateOnOpen())
    } else {
        tryToFindExternalClassInStdlib(binaryClass, fromElement, javaProject)
    }
    
    if (targetEditor !is KotlinEditor) return
    
    val targetKtFile = targetEditor.parsedFile ?: return
    val offset = findDeclarationInParsedFile(descriptor, targetKtFile)
    val start = LineEndUtil.convertLfToDocumentOffset(targetKtFile.getText(), offset, targetEditor.document)
    
    targetEditor.javaEditor.selectAndReveal(start, 0)
}

private fun gotoKotlinDeclaration(element: PsiElement, fromElement: KtElement, fromEditor: KotlinEditor, javaProject: IJavaProject) {
    val targetEditor = findEditorForReferencedElement(element, fromElement, fromEditor, javaProject)
    if (targetEditor !is KotlinEditor) return
    
    val start = element.getTextDocumentOffset(targetEditor.document)
    targetEditor.selectAndReveal(start, 0)
}

private fun tryToFindExternalClassInStdlib(
        binaryClass: VirtualFileKotlinClass,
        fromElement: KtElement,
        javaProject: IJavaProject): IEditorPart? {
    if (KotlinNature.hasKotlinNature(javaProject.project)) {
        // If project has Kotlin nature, then search in stdlib should be done earlier by searching class in the classpath
        return null
    }
    
    val (name, pckg, source) = findSourceForElementInStdlib(binaryClass) ?: return null
    val content = String(source)
    val ktFile = createKtFile(content, KtPsiFactory(fromElement), "dummy.kt")
    
    return openKotlinEditorForExternalFile(content, name, pckg, ktFile)
}

private fun findSourceForElementInStdlib(binaryClass: VirtualFileKotlinClass): ExternalSourceFile? {
    val (sourceName, packagePath) = findSourceFilePath(binaryClass.file)
    
    val source = KotlinSourceIndex.getSource(RUNTIME_SOURCE_MAPPER, sourceName, packagePath, KOTLIN_SOURCE_PATH)
    
    return if (source != null) ExternalSourceFile(sourceName, packagePath.toPortableString(), source) else null
}

private data class ExternalSourceFile(val name: String, val packageFqName: String, val source: CharArray)

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

private fun findSourceFilePath(virtualFile: VirtualFile): Pair<String, IPath> {
    val reader = ClassFileReader(virtualFile.contentsToByteArray(), virtualFile.name.toCharArray())
    val sourceName = String(reader.sourceFileName())
    
    val fileNameWithPackage = Path(String(reader.getName()))
    val packagePath = fileNameWithPackage.removeLastSegments(1)
    
    return Pair(sourceName, packagePath)
}

private fun findEditorPart(
        targetFile: IFile, 
        element: PsiElement,
        javaProject: IJavaProject): IEditorPart? {
    if (targetFile.exists()) return openInEditor(targetFile)
    
    val editor = tryToFindSourceInJavaProject(element, javaProject)
    if (editor != null) {
        return editor
    }
    
    //external jar
    if (targetFile.getFullPath().toOSString().contains("jar")) {
        val elementFile = element.getContainingFile()
        if (elementFile == null) return null
        
        val directory = elementFile.getContainingDirectory()
        return openKotlinEditorForExternalFile(
                elementFile.text,
                elementFile.name,
                getFqNameInsideArchive(directory.toString()),
                elementFile as KtFile)
    }
    
    return null
}

private fun tryToFindSourceInJavaProject(element: PsiElement, javaProject: IJavaProject): AbstractTextEditor? {
    val psiClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java, false) ?: return null
    val targetType = javaProject.findType(psiClass.getQualifiedName()) ?: return null
    
    return EditorUtility.openInEditor(targetType, true) as? AbstractTextEditor?
}

private fun openKotlinEditorForExternalFile(
        sourceText: String,
        sourceName: String,
        packageFqName: String,
        ktFile: KtFile): IEditorPart? {
    val storage = StringStorage(sourceText, sourceName, packageFqName)
    val input = KotlinExternalEditorInput(ktFile, storage)
    return openEditorForExternalFile(input, KotlinExternalReadOnlyEditor.EDITOR_ID)
}

private fun openJavaEditorForExternalFile(
        sourceText: String,
        sourceName: String,
        packageFqName: String): IEditorPart? {
    val storage = StringStorage(sourceText, sourceName, packageFqName)
    val input = StringInput(storage)
    return openEditorForExternalFile(input, JavaUI.ID_CU_EDITOR)
}

private fun openEditorForExternalFile(storageInput: IStorageEditorInput, editorId: String): IEditorPart? {
    val page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
    if (page == null) return null

    val reusedEditor = page.findEditor(storageInput)
    if (reusedEditor != null) {
        page.reuseEditor(reusedEditor as IReusableEditor?, storageInput)
    }

    return page.openEditor(storageInput, editorId)
}

private fun openInEditor(file: IFile): IEditorPart {
    val page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
    return IDE.openEditor(page, file, false)
}

private fun IPath.createSourceMapperWithRoot(): SourceMapper = SourceMapper(this, "", emptyMap<Any, Any>())