package org.jetbrains.kotlin.ui.editors.highlighting;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.ui.text.IColorManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.ITokenScanner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.eclipse.ui.utils.IndenterUtil;
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil;
import org.jetbrains.kotlin.psi.JetFile;

import com.intellij.psi.PsiElement;

public class KotlinTokenScanner implements ITokenScanner {
    private final IFile file;
    private final KotlinTokenSettings settings;
    
    private JetFile jetFile = null;
    private int offset = 0;
    private PsiElement lastElement = null;
    private IDocument document;
    
    public KotlinTokenScanner(@NotNull IFile file, @NotNull IPreferenceStore preferenceStore, @NotNull IColorManager colorManager) {
        this.file = file;
        this.settings = new KotlinTokenSettings(preferenceStore, colorManager);
    }

    @Override
    public void setRange(IDocument document, int offset, int length) {
        this.document = document;
        jetFile = KotlinPsiManager.getKotlinFileIfExist(file, document.get());
        this.offset = LineEndUtil.convertCrToDocumentOffset(document, offset);
    }

    @Override
    public IToken nextToken() {
        lastElement = jetFile.findElementAt(offset);
        if (lastElement != null) {
            offset += lastElement.getTextLength();
            return KotlinToken.create(lastElement, settings);
        }
        
        return new KotlinToken(settings, HighlightingPackage.getEofParam());
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
    
}
