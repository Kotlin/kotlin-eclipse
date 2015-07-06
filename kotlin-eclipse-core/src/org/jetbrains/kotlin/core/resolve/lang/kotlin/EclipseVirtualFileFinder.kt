/*******************************************************************************
* Copyright 2000-2014 JetBrains s.r.o.
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
package org.jetbrains.kotlin.core.resolve.lang.kotlin

import org.eclipse.core.resources.IResource
import org.eclipse.core.runtime.IPath
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.IType
import org.eclipse.jdt.core.JavaModelException
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.core.model.KotlinEnvironment
import org.jetbrains.kotlin.core.resolve.lang.java.EclipseJavaClassFinder
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.kotlin.JvmVirtualFileFinder
import org.jetbrains.kotlin.load.kotlin.JvmVirtualFileFinderFactory
import org.jetbrains.kotlin.load.kotlin.KotlinBinaryClassCache
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.load.kotlin.VirtualFileKotlinClassFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaClass
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaElement
import org.eclipse.jdt.core.dom.ITypeBinding
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaElementUtil
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaClassifier
import org.eclipse.jdt.internal.compiler.util.Util.isClassFileName

public class EclipseVirtualFileFinder(val javaProject: IJavaProject) : VirtualFileKotlinClassFinder(), JvmVirtualFileFinderFactory {
    override public fun findVirtualFileWithHeader(classId: ClassId): VirtualFile? {
        val type = javaProject.findType(classId.getPackageFqName().asString(), classId.getRelativeClassName().asString())
        if (type == null || !isBinaryKotlinClass(type)) return null

        val resource = type.getResource() // if resource != null then it exists in the workspace and then get absolute path
        val path = if (resource != null) resource.getLocation() else type.getPath()

//        In the classpath we can have either path to jar file ot to the class folder
//        Therefore in path might be location to the jar file or to the class file
        return when {
            isClassFileName(path.toOSString()) -> KotlinEnvironment.getEnvironment(javaProject).getVirtualFile(path)

            KotlinEnvironment.getEnvironment(javaProject).isJarFile(path) -> {
            	val relativePath = "${type.getFullyQualifiedName().replace('.', '/')}.class"
	            KotlinEnvironment.getEnvironment(javaProject).getVirtualFileInJar(path, relativePath)
            }

            else -> throw IllegalArgumentException("Virtual file not found for $path")
        }
    }

    private fun isBinaryKotlinClass(type: IType): Boolean = type.isBinary() && !EclipseJavaClassFinder.isInKotlinBinFolder(type)

    private fun classFileName(jClass:JavaClass): String {
        val outerClass = jClass.getOuterClass()
        if (outerClass == null) return jClass.getName().asString()
        return classFileName(outerClass) + "$" + jClass.getName().asString()
    }

    override public fun findKotlinClass(javaClass: JavaClass): KotlinJvmBinaryClass? {
        val fqName = javaClass.getFqName()
        if (fqName == null) return null

        val classId = EclipseJavaElementUtil.computeClassId((javaClass as EclipseJavaClassifier<*>).getBinding())
        if (classId == null) return null

        var file = findVirtualFileWithHeader(classId)
        if (file == null) return null

        if (javaClass.getOuterClass() != null) {
            // For nested classes we get a file of the containing class, to get the actual class file for A.B.C,
            // we take the file for A, take its parent directory, then in this directory we look for A$B$C.class
            file = file.getParent().findChild("${classFileName(javaClass)}.class")
            if (file != null) throw IllegalStateException("Virtual file not found for $javaClass")
        }

        return KotlinBinaryClassCache.getKotlinBinaryClass(file!!)
    }

    override public fun create(scope: GlobalSearchScope): JvmVirtualFileFinder {
        return EclipseVirtualFileFinder(javaProject)
    }
}