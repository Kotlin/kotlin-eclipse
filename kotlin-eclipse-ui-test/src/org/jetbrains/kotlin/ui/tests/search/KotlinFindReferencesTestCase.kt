package org.jetbrains.kotlin.ui.tests.search

import org.jetbrains.kotlin.testframework.editor.KotlinProjectTestCase
import org.junit.Before
import java.io.File
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import org.eclipse.core.commands.ExecutionEvent
import org.eclipse.ui.ISources
import org.eclipse.jface.text.ITextSelection
import org.jetbrains.kotlin.core.references.*
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.eclipse.ui.utils.*
import org.jetbrains.kotlin.testframework.editor.TextEditorTest
import org.eclipse.jdt.core.IJavaElement
import org.eclipse.jdt.core.search.SearchEngine
import org.eclipse.jdt.core.search.SearchPattern
import org.eclipse.jdt.core.search.IJavaSearchConstants
import org.eclipse.jdt.core.search.SearchRequestor
import org.eclipse.jdt.core.search.SearchMatch
import java.util.ArrayList
import org.jetbrains.kotlin.core.model.KotlinEnvironment
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils
import org.eclipse.jdt.internal.ui.search.JavaSearchScopeFactory
import org.eclipse.jdt.ui.search.ElementQuerySpecification
import org.eclipse.jdt.internal.ui.search.JavaSearchQuery
import org.eclipse.search.ui.ISearchResult
import org.eclipse.core.runtime.NullProgressMonitor
import org.jetbrains.kotlin.psi.KtElement
import org.eclipse.search.ui.text.AbstractTextSearchResult
import org.eclipse.search.ui.text.Match
import org.eclipse.jdt.internal.ui.search.JavaElementMatch
import org.jetbrains.kotlin.ui.search.KotlinElementMatch
import com.intellij.openapi.util.text.StringUtilRt
import org.junit.Assert
import com.intellij.openapi.util.text.StringUtil
import org.eclipse.jdt.core.ISourceReference
import org.eclipse.core.resources.IFile
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor
import org.eclipse.jdt.internal.ui.search.AbstractJavaSearchResult
import org.eclipse.core.resources.IResource
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.eclipse.jface.text.AbstractDocument
import org.jetbrains.kotlin.core.references.createReference
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.eclipse.ui.utils.getTextDocumentOffset
import kotlin.properties.Delegates
import org.eclipse.ui.editors.text.EditorsUI
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants
import java.util.regex.Pattern
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility
import org.eclipse.jdt.core.ICompilationUnit
import org.jetbrains.kotlin.core.model.KotlinAnalysisProjectCache
import org.eclipse.jdt.ui.search.QuerySpecification
import org.eclipse.jdt.core.search.IJavaSearchScope
import org.jetbrains.kotlin.ui.commands.findReferences.createQuerySpecification

abstract class KotlinFindReferencesTestCase : KotlinProjectTestCase() {
    @Before public fun configure() {
        configureProjectWithStdLib()
    }
    
    protected fun doTest(filePath: String) {
        val (editor, sourceFiles, resultFile) = configureSourceFiles(filePath)
        
        KotlinTestUtils.joinBuildThread()
        
        val scope = SearchEngine.createWorkspaceScope()
        
        val querySpecification = createQuerySpecificationBy(editor, scope)
        val searchQuery = JavaSearchQuery(querySpecification)
        searchQuery.run(NullProgressMonitor())
        
        val searchResult = searchQuery.getSearchResult()
        checkResults(searchResult, resultFile, sourceFiles)
    }
    
    private fun createQuerySpecificationBy(editor: TextEditorTest, scope: IJavaSearchScope) = 
        if (editor.getEditingFile().getFileExtension() == "java") {
            val root = EditorUtility.getEditorInputJavaElement(editor.getEditor(), false) as ICompilationUnit
            val javaElement = root.getElementAt(editor.getCaretOffset())
            
            ElementQuerySpecification(javaElement, IJavaSearchConstants.REFERENCES, scope, null)
        } else {
            val jetFile = KotlinPsiManager.INSTANCE.getParsedFile(editor.getEditingFile())
            val element = jetFile.findElementByDocumentOffset(editor.getCaretOffset(), editor.getDocument())!!
            val jetElement = PsiTreeUtil.getNonStrictParentOfType(element, KtElement::class.java)!!
            
            createQuerySpecification(jetElement, editor.getTestJavaProject().getJavaProject(), scope, "")
        }
    
    private fun checkResults(searchResult: ISearchResult, resultFile: File, sourceFiles: List<TestFile>) {
        if (searchResult !is AbstractJavaSearchResult) throw RuntimeException()
        
        val actualResults = searchResult.getElements().flatMap { searchElement ->
            searchResult.getMatches(searchElement).map { match -> 
                when (match) {
                    is JavaElementMatch -> {
                        val file = searchResult.getFile(match.getElement())!!
                        val testFile = sourceFiles.firstOrNull { it.file == file }
                        if (testFile == null) return@map null
                        renderReference(testFile, match.getOffset())
                    }
                    is KotlinElementMatch -> {
                        val file = KotlinPsiManager.getEclispeFile(match.jetElement.getContainingKtFile())
                        val testFile = sourceFiles.first { it.file == file }
                        renderKotlinReference(testFile, match.jetElement)
                    }
                    else -> throw RuntimeException()
                }
            }.filterNotNull()
        }
        
        val expectedResults = loadResultsFile(resultFile)
        
        val agreement = expectedResults.all { expectedResult ->
            actualResults.any { actualResult ->
                (expectedResult.fileName == null || expectedResult == actualResult) && 
                    expectedResult.lineNumber == actualResult.lineNumber && 
                	expectedResult.offsetInLine == actualResult.offsetInLine
            }
        }
        
        Assert.assertTrue("$expectedResults are not equals to $actualResults", agreement)
    }
    
    private fun loadResultsFile(resultFile: File): List<TestResult> {
        val fileNameRegex = "\\[(.+)\\]".toRegex()
        val offsetRegex = "\\((\\d+):\\s*(\\d+)\\)".toRegex()
        return FileUtil.loadFile(resultFile).splitToSequence("\n")
            .filter { it.isNotBlank() }
            .map { line ->
                val fileNameMatch = fileNameRegex.find(line)
                val fileName = if (fileNameMatch != null) fileNameMatch.groups[1]?.value as String else null
                
                val offsetMatch= offsetRegex.find(line)!!
	            TestResult(
	                fileName, 
	                offsetMatch.groups[1]!!.value.toInt(), 
	                offsetMatch.groups[2]!!.value.toInt())
        	}.toList()
    }
    
    private fun renderKotlinReference(testFile: TestFile, jetElement: KtElement): TestResult {
        val document = EditorUtil.getDocument(testFile.file)
        return renderReference(testFile, jetElement.getTextDocumentOffset(document))
    }
    
    private fun renderReference(testFile: TestFile, offset: Int): TestResult {
        val document = EditorUtil.getDocument(testFile.file)
        if (document is AbstractDocument) {
        	val lineNumber = document.getLineOfOffset(offset) + 1
			val lineInformation = document.getLineInformationOfOffset(offset)
            return TestResult(testFile.name, lineNumber, offset - lineInformation.getOffset() + 1)
        }
        
        throw RuntimeException()
    }
    
    private fun configureSourceFiles(filePath: String): TestConfiguration {
        val mainFile = File(filePath)
        val mainFileName = mainFile.getName()
        val mainFileText = FileUtil.loadFile(mainFile)
        
        val editor = configureEditor(mainFileName, mainFileText)
        
        val prefix = mainFileName.substring(0, mainFileName.indexOf('.') + 1)
        
        val rootPath = filePath.substring(0, filePath.lastIndexOf("/") + 1)
        val rootDir = File(rootPath)
        val extraFiles = rootDir.listFiles { dir, name ->
        	if (!name.startsWith(prefix) || name.equals(mainFileName)) return@listFiles false
            
            val ext = FileUtilRt.getExtension(name)
			ext == "kt" || ext == "java"
        }
        
        val sourceFiles = ArrayList<TestFile>()
        extraFiles.mapTo(sourceFiles) { TestFile(createSourceFile(it.getName(), FileUtil.loadFile(it)), it.getName()) }
        sourceFiles.add(TestFile(editor.getEditingFile(), mainFileName))
        
        return TestConfiguration(editor, sourceFiles, File(rootPath, "${prefix}results.txt"))
    }
    
    data class TestResult(val fileName: String?, val lineNumber: Int, val offsetInLine: Int)
    
    data class TestConfiguration(val editor: TextEditorTest, val sourceFiles: List<TestFile>, val resultFile: File)
    
    data class TestFile(val file: IFile, val name: String)
}