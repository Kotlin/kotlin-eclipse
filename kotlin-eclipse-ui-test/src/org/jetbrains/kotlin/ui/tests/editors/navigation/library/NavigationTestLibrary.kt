package org.jetbrains.kotlin.ui.tests.editors.navigation.library

import org.jetbrains.kotlin.testframework.utils.TestJavaProject
import org.jetbrains.kotlin.core.compiler.KotlinCompiler
import org.eclipse.core.runtime.Path
import java.io.File
import org.eclipse.jdt.core.IPackageFragmentRoot
import org.eclipse.core.resources.IFolder
import java.io.InputStream
import org.eclipse.core.runtime.CoreException
import java.util.jar.JarOutputStream
import java.io.FileOutputStream
import java.util.jar.Manifest
import java.util.jar.JarEntry
import java.io.FileInputStream
import java.io.BufferedInputStream
import kotlin.io.use
import org.eclipse.jdt.internal.compiler.util.Util.isClassFileName
import org.eclipse.core.resources.IResource
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import org.jetbrains.kotlin.core.launch.KotlinCLICompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import java.io.Reader
import org.jetbrains.kotlin.core.compiler.KotlinCompiler.KotlinCompilerResult
import org.jetbrains.kotlin.core.launch.CompilerOutputData
import java.util.ArrayList
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.core.launch.CompilerOutputParser
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import java.io.BufferedReader
import java.io.StringReader
import org.jetbrains.kotlin.core.utils.ProjectUtils

public object NavigationTestLibrary {
    private val srcFolderName = "src"
    private val targetFolderName = "target"
    
    public val commonArtifactName: String = "kotlin-navigation-test-lib"
    
    private val testDataRootFolder = File("testData/navigation/lib/")
    private val srcPath = File(testDataRootFolder, srcFolderName)
    
    private val libraryFile: File
    private val sourceArchiveFile: File
    
    public val libraryPath: String
        get() = libraryFile.getAbsolutePath()
    public val sourceArchivePath: String
        get() = sourceArchiveFile.getAbsolutePath()
    
    init {
        val targetFolder = File(testDataRootFolder, targetFolderName)
        if (!targetFolder.exists()) {
            targetFolder.mkdir()
        }
        libraryFile = File(targetFolder, "${commonArtifactName}.jar")
        sourceArchiveFile = File(targetFolder, "${commonArtifactName}-sources.zip")
        if (!libraryFile.exists()) {
            runCompiler(libraryFile)
        } 
        if (!sourceArchiveFile.exists()) {
            createSourceArchive(sourceArchiveFile, srcPath)            
        }
    }
    
    public fun clean() {
        libraryFile.delete()
        sourceArchiveFile.delete();
    }
    
    private fun createSourceArchive(targetFile: File, contentsDir: File) {
        val stream = ZipOutputStream(FileOutputStream(targetFile))
        stream.use {
            writeEntriesToJarRecursively(stream, contentsDir, "")
        }
    }
    
    private fun writeEntriesToJarRecursively(stream: ZipOutputStream, contentDir: File, pathPrefix: String) {
        contentDir.listFiles().forEach {
            val entryName = pathPrefix + it.getName() + if (it.isDirectory()) "/" else ""
            when {
                it.isDirectory() -> { 
                    stream.putNextEntry(ZipEntry(entryName))
                    stream.closeEntry()
                    writeEntriesToJarRecursively(stream, it, entryName) 
                }
                it.isFile() -> {
                    stream.putNextEntry(ZipEntry(entryName))
                    val inStream = BufferedInputStream(FileInputStream(it));
                    inStream.use { 
                        it.copyTo(stream)
                    }
                    stream.closeEntry()
                }
            }
         }
    }
    
    private fun runCompiler(targetFile: File) {
        val targetPath: String = Path(targetFile.getAbsolutePath()).toOSString()
        val outputStream = ByteArrayOutputStream();
        val out = PrintStream(outputStream);
        
        KotlinCLICompiler.doMain(K2JVMCompiler(), out, arrayOf("-d", targetPath, "-kotlin-home", ProjectUtils.KT_HOME, srcPath.getAbsolutePath()))
    }
}
