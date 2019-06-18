package org.jetbrains.kotlin.ui.tests.editors.navigation.library

import org.eclipse.core.runtime.Path
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.core.launch.KotlinCLICompiler
import org.jetbrains.kotlin.core.utils.ProjectUtils
import org.jetbrains.kotlin.utils.PathUtil
import org.junit.Assert
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

public data class TestLibraryData(val libPath: String, val srcPath: String)

public fun getTestLibrary() = 
        NavigationTestLibrary().let { 
            TestLibraryData(it.libraryPath, it.sourceArchivePath)
         }

public fun TestLibraryData.clean() {
    File(libPath).delete()
    File(srcPath).delete()
}

private class NavigationTestLibrary {
    private val srcFolderName = "src"
    private val targetFolderName = "target"
    
    public val commonArtifactName: String = "kotlin-navigation-test-lib"
    
    private val testDataRootFolder = File("testData/navigation/lib/")
    private val srcPath = File(testDataRootFolder, srcFolderName)
    
    private val libraryFile: File
    private val sourceArchiveFile: File
    
    public val libraryPath: String
        get() {
            return libraryFile.getAbsolutePath()
        }
    
    public val sourceArchivePath: String
        get() {
            return sourceArchiveFile.getAbsolutePath()
        }
    
    init {
        val targetFolder = File(testDataRootFolder, targetFolderName)
        if (!targetFolder.exists()) {
            targetFolder.mkdir()
        }
        libraryFile = File(targetFolder, "${commonArtifactName}.jar")
        if (!libraryFile.exists()) {
            runCompiler(libraryFile)
        }
        
        sourceArchiveFile = File(targetFolder, "${commonArtifactName}-sources.zip")
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
        
        val exitCode = KotlinCLICompiler.doMain(K2JVMCompiler(), out, 
                arrayOf("-d", targetPath, "-kotlin-home", ProjectUtils.ktHome,
                    "-Xplugin=" + ProjectUtils.buildLibPath(PathUtil.KOTLIN_SCRIPTING_COMPILER_PLUGIN_NAME),
                    srcPath.getAbsolutePath()))
        Assert.assertTrue(
                "Could not compile test library, exitCode = $exitCode\n ${outputStream.toString()}", 
                exitCode == ExitCode.OK)
    }
}
