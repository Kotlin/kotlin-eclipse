package org.jetbrains.kotlin.ui.editors.highlighting;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.text.IColorManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.ITokenScanner;
import org.eclipse.jface.text.rules.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.core.model.KotlinEnvironment;
import org.jetbrains.kotlin.core.model.KotlinNature;
import org.jetbrains.kotlin.eclipse.ui.utils.IndenterUtil;
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.psi.JetPsiFactory;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;

public class KotlinTokenScanner implements ITokenScanner {
    private final KotlinTokensFactory kotlinTokensFactory;
    
    private JetFile jetFile = null;
    private int offset = 0;
    private int rangeEnd = 0;
    private PsiElement lastElement = null;
    private IDocument document;
    
    public KotlinTokenScanner(@NotNull IPreferenceStore preferenceStore, @NotNull IColorManager colorManager) {
        kotlinTokensFactory = new KotlinTokensFactory(preferenceStore, colorManager);
    }
    
    @Override
    public void setRange(IDocument document, int offset, int length) {
        this.document = document;
        jetFile = createJetFile(document);
        this.offset = LineEndUtil.convertCrToDocumentOffset(document, offset);
        this.rangeEnd = this.offset + length;
        this.lastElement = null;
    }

    @Override
    public IToken nextToken() {
        if (lastElement != null) {
            if (lastElement.getTextOffset() > rangeEnd) {
                return Token.EOF;
            }
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
    private static JetFile createJetFile(@NotNull IDocument document) {
        KotlinEnvironment environment = getAnyKotlinEnvironment();
        if (environment != null) {
            Project ideaProject = environment.getProject();
            return new JetPsiFactory(ideaProject).createFile(StringUtil.convertLineSeparators(document.get()));
        }
        
        return null;
    }
    
    @Nullable
    private static KotlinEnvironment getAnyKotlinEnvironment() {
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        for (IProject project : projects) {
            if (KotlinNature.hasKotlinNature(project)) {
                return KotlinEnvironment.getEnvironment(JavaCore.create(project));
            }
        }
        
        return null;
    }
}
