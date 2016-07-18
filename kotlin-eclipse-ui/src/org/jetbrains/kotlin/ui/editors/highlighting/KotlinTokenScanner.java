package org.jetbrains.kotlin.ui.editors.highlighting;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.ui.text.IColorManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.ITokenScanner;
import org.eclipse.jface.text.rules.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.core.model.KotlinCommonEnvironment;
import org.jetbrains.kotlin.core.model.KotlinEnvironment;
import org.jetbrains.kotlin.core.model.KotlinEnvironmentKt;
import org.jetbrains.kotlin.core.model.KotlinNature;
import org.jetbrains.kotlin.eclipse.ui.utils.IndenterUtil;
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtPsiFactory;
import org.jetbrains.kotlin.ui.editors.KotlinEditor;
import org.jetbrains.kotlin.ui.formatter.KotlinFormatterKt;

import com.intellij.psi.PsiElement;

public class KotlinTokenScanner implements ITokenScanner {
    private final KotlinEditor editor;
    private final KotlinTokensFactory kotlinTokensFactory;
    
    private KtFile jetFile = null;
    private int offset = 0;
    private int rangeEnd = 0;
    private PsiElement lastElement = null;
    private IDocument document;
    
    private KotlinTokenScanner(
            @NotNull IPreferenceStore preferenceStore, 
            @NotNull IColorManager colorManager,
            @Nullable KotlinEditor editor) {
        this.editor = editor;
        kotlinTokensFactory = new KotlinTokensFactory(preferenceStore, colorManager);
    }
    
    public static KotlinTokenScanner createScanner(
            @NotNull IPreferenceStore preferenceStore, 
            @NotNull IColorManager colorManager,
            @NotNull KotlinEditor editor) {
        return new KotlinTokenScanner(preferenceStore, colorManager, editor);
    }
    
    public static KotlinTokenScanner createScannerForCompareViewOfKtSourceFile(
            @NotNull IPreferenceStore preferenceStore, 
            @NotNull IColorManager colorManager) {
        return new KotlinTokenScanner(preferenceStore, colorManager, null);
    }
    
    @Override
    public void setRange(IDocument document, int offset, int length) {
        this.document = document;
        jetFile = createKtFile(document);
        this.offset = LineEndUtil.convertCrToDocumentOffset(document, offset);
        this.rangeEnd = LineEndUtil.convertCrToDocumentOffset(document, offset + length);
        this.lastElement = null;
    }

    @Override
    public IToken nextToken() {
        if (rangeEnd <= offset) {
            return Token.EOF;
        }
        
        if (jetFile == null) return Token.EOF;
        
        lastElement = jetFile.findElementAt(offset);
        if (lastElement != null) {
            offset = lastElement.getTextRange().getEndOffset();
            return kotlinTokensFactory.getToken(lastElement);
        }
        
        return Token.EOF;
    }

    @Override
    public int getTokenOffset() {
        return LineEndUtil.convertLfToDocumentOffset(jetFile.getText(), lastElement.getTextOffset(), document);
    }

    @Override
    public int getTokenLength() {
        int length = lastElement.getTextLength();
        if (TextUtilities.getDefaultLineDelimiter(document).length() > 1) {
            length += IndenterUtil.getLineSeparatorsOccurences(lastElement.getText());
        }
        return length;
    }
    
    @Nullable
    private KtFile createKtFile(@NotNull IDocument document) {
        if (editor != null) {
            IFile eclipseFile = editor.getEclipseFile();
            if (eclipseFile != null) {
                return createKtFile(KotlinEnvironmentKt.getEnvironment(eclipseFile), eclipseFile.getName());
            } 
            
            if (editor.getJavaEditor().isEditorInputReadOnly()) {
                return editor.getParsedFile();
            }
        } 
        
        return createKtFile(getAnyKotlinEnvironmentForSourceFile(), "dummy.kt");
    }
    
    private KtFile createKtFile(@Nullable KotlinCommonEnvironment environment, String fileName) {
        if (environment == null) {
            return null;
        }
        
        return KotlinFormatterKt.createKtFile(document.get(), new KtPsiFactory(environment.getProject()), fileName);
    }
    
    @Nullable
    private static KotlinEnvironment getAnyKotlinEnvironmentForSourceFile() {
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        for (IProject project : projects) {
            if (project.isAccessible() && KotlinNature.hasKotlinNature(project)) {
                return KotlinEnvironment.getEnvironment(project);
            }
        }
        
        return null;
    }
}
