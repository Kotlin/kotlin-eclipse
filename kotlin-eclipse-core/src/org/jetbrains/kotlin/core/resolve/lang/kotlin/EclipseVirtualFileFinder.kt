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

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.IType
import org.eclipse.jdt.internal.compiler.util.Util.isClassFileName
import org.jetbrains.kotlin.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.core.model.KotlinEnvironment
import org.jetbrains.kotlin.core.resolve.lang.java.EclipseJavaClassFinder
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaClassifier
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaElementUtil
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.kotlin.KotlinBinaryClassCache
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.MetadataPackageFragment
import java.io.InputStream
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndex
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory

class EclipseVirtualFileFinder(
        private val javaProject: IJavaProject,
        private val scope: GlobalSearchScope) : VirtualFileFinder() {
    
    val index: JvmDependenciesIndex
        get() = KotlinEnvironment.getEnvironment(javaProject.getProject()).index
    
    override fun findMetadata(classId: ClassId): InputStream? {
        assert(!classId.isNestedClass) { "Nested classes are not supported here: $classId" }

        return findBinaryClass(
                classId,
                classId.shortClassName.asString() + MetadataPackageFragment.DOT_METADATA_FILE_EXTENSION)?.inputStream
    }

    override fun hasMetadataPackage(fqName: FqName): Boolean {
        var found = false
        
        val index = KotlinEnvironment.getEnvironment(javaProject.getProject()).index
        
        index.traverseDirectoriesInPackage(fqName, continueSearch = { dir, rootType ->
            found = found or dir.children.any { it.extension == MetadataPackageFragment.METADATA_FILE_EXTENSION }
            !found
        })
        return found
    }

    override fun findBuiltInsData(packageFqName: FqName): InputStream? {
        val fileName = BuiltInSerializerProtocol.getBuiltInsFileName(packageFqName)

        // "<builtins-metadata>" is just a made-up name
        // JvmDependenciesIndex requires the ClassId of the class which we're searching for, to cache the last request+result
        val classId = ClassId(packageFqName, Name.special("<builtins-metadata>"))
        
        return index.findClass(classId, acceptedRootTypes = JavaRoot.OnlyBinary) { dir, rootType ->
            dir.findChild(fileName)?.check(VirtualFile::isValid)
        }?.check { it in scope && it.isValid }?.inputStream
    }

    override public fun findVirtualFileWithHeader(classId: ClassId): VirtualFile? {
        val type = javaProject.findType(classId.getPackageFqName().asString(), classId.getRelativeClassName().asString())
        if (type == null || !isBinaryKotlinClass(type)) return null

        val resource = type.getResource() // if resource != null then it exists in the workspace and then get absolute path
        val path = if (resource != null) resource.getLocation() else type.getPath()

        val eclipseProject = javaProject.project
//        In the classpath we can have either path to jar file ot to the class folder
//        Therefore in path might be location to the jar file or to the class file
        return when {
            isClassFileName(path.toOSString()) -> KotlinEnvironment.getEnvironment(eclipseProject).getVirtualFile(path)

            KotlinEnvironment.getEnvironment(eclipseProject).isJarFile(path) -> {
            	val relativePath = "${type.getFullyQualifiedName().replace('.', '/')}.class"
	            KotlinEnvironment.getEnvironment(eclipseProject).getVirtualFileInJar(path, relativePath)
            }

            else -> throw IllegalArgumentException("Virtual file not found for $path")
        }
    }
    
    private fun isBinaryKotlinClass(type: IType): Boolean = type.isBinary() && !EclipseJavaClassFinder.isInKotlinBinFolder(type)

    private fun classFileName(jClass:JavaClass): String {
        val outerClass = jClass.outerClass
        if (outerClass == null) return jClass.name.asString()
        return classFileName(outerClass) + "$" + jClass.name.asString()
    }
    
    private fun findBinaryClass(classId: ClassId, fileName: String): VirtualFile? =
            index.findClass(classId, acceptedRootTypes = JavaRoot.OnlyBinary) { dir, rootType ->
                dir.findChild(fileName)?.check(VirtualFile::isValid)
            }?.check { it in scope }

    override public fun findKotlinClass(javaClass: JavaClass): KotlinJvmBinaryClass? {
        val fqName = javaClass.fqName
        if (fqName == null) return null

        val classId = EclipseJavaElementUtil.computeClassId((javaClass as EclipseJavaClassifier<*>).binding)
        if (classId == null) return null

        var file = findVirtualFileWithHeader(classId)
        if (file == null) return null

        if (javaClass.outerClass != null) {
            // For nested classes we get a file of the containing class, to get the actual class file for A.B.C,
            // we take the file for A, take its parent directory, then in this directory we look for A$B$C.class
            file = file.getParent().findChild("${classFileName(javaClass)}.class")
            if (file != null) throw IllegalStateException("Virtual file not found for $javaClass")
        }

        return KotlinBinaryClassCache.getKotlinBinaryClass(file!!)
    }
}

class EclipseVirtualFileFinderFactory(private val project: IJavaProject) : VirtualFileFinderFactory {
    override fun create(scope: GlobalSearchScope): VirtualFileFinder = EclipseVirtualFileFinder(project, scope)
}

fun <T: Any> T.check(predicate: (T) -> Boolean): T? = if (predicate(this)) this else null