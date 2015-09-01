package org.jetbrains.kotlin.ui.tests.editors.navigation

import java.io.File
import org.eclipse.core.runtime.Path
import org.eclipse.jdt.core.JavaModelException
import org.jetbrains.kotlin.testframework.editor.KotlinProjectTestCase
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.jetbrains.kotlin.ui.editors.KotlinEditor
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.ui.tests.editors.navigation.library.NavigationTestLibrary
import org.junit.AfterClass
import kotlin.platform.platformStatic

public open class KotlinNavigationToLibraryTestCase: KotlinSourcesNavigationTestCase() {
    override fun getParsedFile(editor: KotlinEditor): JetFile =
            editor.parsedFile!!

    override val testDataPath: String =
        "testData/navigation/lib"

    Before
    override fun configure() {
        super.configure()
        try
        {
            val libPath = Path(NavigationTestLibrary.libraryPath)
            val srcPath = Path(NavigationTestLibrary.sourceArchivePath)
            getTestProject().addLibrary(libPath, srcPath)
        }
        catch (e:JavaModelException) {
            throw RuntimeException(e)
        }
    }
    
    companion object {
        @AfterClass
        @platformStatic
        fun afterAllTests() {
            NavigationTestLibrary.clean()
        }
    }
}