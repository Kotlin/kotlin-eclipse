package org.jetbrains.kotlin.core.tests.diagnostics;

import junit.framework.TestCase;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.KtFile;
import org.junit.Assert;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtilRt;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.LightVirtualFile;

public class JetLightFixture {

	protected static void ensureParsed(PsiFile file) {
        file.accept(new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                element.acceptChildren(this);
            }
        });
    }
	
	public static KtFile createCheckAndReturnPsiFile(String testName, String fileName, String text, Project project) {
		text = StringUtilRt.convertLineSeparators(text);
        KtFile myFile = createPsiFile(testName, fileName, text, project);
        ensureParsed(myFile);
        TestCase.assertEquals("light virtual file text mismatch", text, ((LightVirtualFile) myFile.getVirtualFile()).getContent().toString());
        TestCase.assertEquals("virtual file text mismatch", text, ((LightVirtualFile) myFile.getVirtualFile()).getContent());
//        TODO: Uncomment and fix error
//        TestCase.assertEquals("doc text mismatch", text, myFile.getViewProvider().getDocument().getText());
        TestCase.assertEquals("psi text mismatch", text, myFile.getText());
        return myFile;
    }
	
	public static KtFile createPsiFile(@Nullable String testName, @Nullable String fileName, String text, Project project) {
        if (fileName == null) {
            Assert.assertNotNull(testName);
            fileName = testName + ".kt";
        }
        
        return JetTestUtils.createFile(fileName, text, project);
    }
}
