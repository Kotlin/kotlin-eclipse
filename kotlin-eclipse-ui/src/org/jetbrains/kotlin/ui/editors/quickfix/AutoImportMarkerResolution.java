package org.jetbrains.kotlin.ui.editors.quickfix;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetImportDirective;
import org.jetbrains.jet.lang.psi.JetNamespaceHeader;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.parser.KotlinParser;
import org.jetbrains.kotlin.ui.editors.KotlinEditor;
import org.jetbrains.kotlin.utils.EditorUtil;
import org.jetbrains.kotlin.utils.IndenterUtil;
import org.jetbrains.kotlin.utils.LineEndUtil;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

public class AutoImportMarkerResolution implements IMarkerResolution2 {

    private final IType type;
    
    public AutoImportMarkerResolution(IType type) {
        this.type = type;
    }

    @Override
    public String getLabel() {
        return "Import '" + type.getElementName() + "' (" + type.getPackageFragment().getElementName() + ")";
    }

    @Override
    public void run(IMarker marker) {
        KotlinEditor editor = getActiveEditor();
        if (editor == null) {
            return;
        }
        
        IDocument document = editor.getViewer().getDocument();

        PsiElement psiElement = findNodeToNewImport(EditorUtil.getFile(editor));
        int breakLineBefore = computeBreakLineBeforeImport(psiElement);
        int breakLineAfter = computeBreakLineAfterImport(psiElement);
        
        String newImport = "import " + type.getFullyQualifiedName('.');
        newImport = IndenterUtil.createWhiteSpace(0, breakLineBefore) + newImport + IndenterUtil.createWhiteSpace(0, breakLineAfter);
        try {
            document.replace(getOffset(psiElement, editor), 0, newImport);
        } catch (BadLocationException e) {
            KotlinLogger.logError(e);
        } 
    }
    
    private int getOffset(PsiElement element, AbstractTextEditor editor) {
        int offset = 0;
        if (element != null) {
            PsiFile parsedFile = KotlinPsiManager.INSTANCE.getParsedFile(EditorUtil.getFile(editor));
            offset = LineEndUtil.convertLfToOsOffset(parsedFile.getText(), element.getTextRange().getEndOffset());
        }
        
        return offset;
    }
    
    private int computeBreakLineAfterImport(PsiElement element) {
        int countBreakLine = 0;
        if (element instanceof JetNamespaceHeader) {
            ASTNode nextNode = element.getNode().getTreeNext();
            if (nextNode.getElementType().equals(JetTokens.WHITE_SPACE)) {
                int countBreakLineAfterHeader = IndenterUtil.getLineSeparatorsOccurences(nextNode.getText());
                if (countBreakLineAfterHeader == 0) {
                    countBreakLine = 2;
                } else if (countBreakLineAfterHeader == 1) {
                    countBreakLine = 1;
                }
            } else {
                countBreakLine = 2;
            }
        }
        
        return countBreakLine;
    }
    
    private int computeBreakLineBeforeImport(PsiElement element) {
        int countBreakLine = 0;
        if (element instanceof JetNamespaceHeader) {
            if (!element.getText().isEmpty()) {
                countBreakLine = 2;
            }
        } else {
            countBreakLine = 1;
        }
        
        return countBreakLine;
    }
    
    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public Image getImage() {
        return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_IMPDECL);
    }
    
    private PsiElement findNodeToNewImport(IFile file) {
        JetFile jetFile = (JetFile) KotlinParser.getPsiFile(file);
        List<JetImportDirective> jetImportDirective = jetFile.getImportDirectives();
        if (jetImportDirective != null && !jetImportDirective.isEmpty()) {
            return jetImportDirective.get(jetImportDirective.size() - 1);
        }
        
        return jetFile.getNamespaceHeader();
    }
    
    private KotlinEditor getActiveEditor() {
        IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        
        if (workbenchWindow == null) {
            return null;
        }
        
        return (KotlinEditor) workbenchWindow.getActivePage().getActiveEditor();
    }
}