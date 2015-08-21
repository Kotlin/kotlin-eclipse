package org.jetbrains.kotlin.ui.tests.editors.navigation;

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ui.PlatformUI;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.model.KotlinEnvironment;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.psi.JetPsiFactory;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilPackage;
import org.jetbrains.kotlin.testframework.editor.KotlinEditorTestCase;
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils;
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor;
import org.jetbrains.kotlin.ui.editors.KotlinOpenDeclarationAction;
import org.junit.Assert;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.util.PsiTreeUtil;

public class KotlinBuiltInsReferenceResolverTestCase extends KotlinEditorTestCase {
    
    private static final String TEST_DATA_PATH = "common_testData/ide/resolve/builtins";
    private static final String KT_FILE_EXTENSION = ".kt";
    private static final String TEST_PREFIX = "test";

    public void doAutoTest() {
        String inputFileName = getFileName();
        String fileText = getText(TEST_DATA_PATH  + File.separator + inputFileName);
        int referenceOffset = getReferenceOffset(fileText);
        fileText = removeTags(fileText);
        
        testEditor = configureEditor(inputFileName, fileText);
        IFile editorFile = createSourceFile(inputFileName, fileText);
        
        JetFile initialFile = KotlinPsiManager.INSTANCE.getParsedFile(editorFile);
        IJavaProject javaProject = testEditor.getTestJavaProject().getJavaProject();
        JavaEditor editor = testEditor.getEditor();
        testEditor.setCaret(referenceOffset);
        
        KotlinOpenDeclarationAction openAction = new KotlinOpenDeclarationAction((KotlinFileEditor)editor);
        openAction.run(new TextSelection(KotlinTestUtils.getCaret(editor), 0));
        
        JavaEditor activeEditor = (JavaEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
        assertWithEditor(initialFile, activeEditor, javaProject);
    }

    private void assertWithEditor(JetFile initialFile, JavaEditor javaEditor, IJavaProject javaProject) {
        List<PsiComment> comments = PsiTreeUtil.getChildrenOfTypeAsList(initialFile, PsiComment.class);
        String[] expectedTarget = comments.get(comments.size() - 1).getText().substring(2).split(":");
        Assert.assertEquals(2, expectedTarget.length);
        String expectedFile = expectedTarget[0].replace('/', File.separatorChar);
        String expectedName = expectedTarget[1];
        
        JetFile jetFile = getFileFromEditor(javaEditor, javaProject);
        
        Assert.assertTrue(javaEditor instanceof KotlinFileEditor);
        KotlinFileEditor editor = (KotlinFileEditor) javaEditor;
        
        int editorOffset = editor.getViewer().getTextWidget().getCaretOffset();
        
        PsiNamedElement expression = PsiUtilPackage.getNonStrictParentOfType(jetFile.findElementAt(editorOffset), PsiNamedElement.class);
        
        Assert.assertEquals(expectedFile, editor.getTitleToolTip());
        Assert.assertEquals(expectedName, expression.getName());
    }

    private JetFile getFileFromEditor(JavaEditor editor, IJavaProject javaProject) {
        KotlinEnvironment environment = KotlinEnvironment.getEnvironment(javaProject);
        Project ideaProject = environment.getProject();
        JetFile jetFile = new JetPsiFactory(ideaProject).createFile(StringUtil.convertLineSeparators(editor.getViewer().getTextWidget().getText()));
        return jetFile;
    }
    
    private String getFileName() {
        String inputFileName = name.getMethodName();
        char filenameAsArray[] = inputFileName.substring(TEST_PREFIX.length()).toCharArray();
        filenameAsArray[0] = Character.toLowerCase(filenameAsArray[0]);
        return new String(filenameAsArray) + KT_FILE_EXTENSION;
    }
    
    private int getReferenceOffset(String fileText) {
        return fileText.indexOf(REFERENCE_TAG);
    }
}
