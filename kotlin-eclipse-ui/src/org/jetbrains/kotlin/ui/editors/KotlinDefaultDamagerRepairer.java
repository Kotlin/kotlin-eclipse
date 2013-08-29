package org.jetbrains.kotlin.ui.editors;


import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.ui.text.IColorManager;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.ITokenScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.utils.EditorUtil;
import org.jetbrains.kotlin.utils.LineEndUtil;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;

public class KotlinDefaultDamagerRepairer extends DefaultDamagerRepairer {
    
    private final AbstractTextEditor editor;
    
    public KotlinDefaultDamagerRepairer(ITokenScanner scanner, AbstractTextEditor editor) {
        super(scanner);
        this.editor = editor;
    }

    @Override
    public void createPresentation(TextPresentation presentation, ITypedRegion region) {
        if (fScanner == null) {
            addRange(presentation, region.getOffset(), region.getLength(), fDefaultTextAttribute);
            return;
        }

        int lastStart = region.getOffset();
        int length = 0;
        boolean firstToken= true;
        IToken lastToken = Token.UNDEFINED;
        TextAttribute lastAttribute= getTokenTextAttribute(lastToken);

        fScanner.setRange(fDocument, lastStart, region.getLength());
        ColorManager colorManager = new ColorManager();
        
        IFile file = EditorUtil.getFile(editor);
        KotlinPsiManager.INSTANCE.updatePsiFile(file, fDocument.get());
        PsiFile parsedFile = KotlinPsiManager.INSTANCE.getParsedFile(file);
        
        while (true) {
            IToken token = fScanner.nextToken();
            if (token.isEOF()) {
                break;
            }
            
            TextAttribute attribute = getTokenTextAttribute(token);
            if (!Scanner.STRING.equals(token)) {
                attribute = getAttributeForElementAt(parsedFile, fScanner.getTokenOffset(), colorManager);
            }

            if (lastAttribute != null && lastAttribute.equals(attribute)) {
                length += fScanner.getTokenLength();
                firstToken= false;
            } else {
                if (!firstToken) {
                    addRange(presentation, lastStart, length, lastAttribute);
                }
                firstToken = false;
                lastToken = token;
                lastAttribute = attribute;
                lastStart = fScanner.getTokenOffset();
                length = fScanner.getTokenLength();
            }
        }

        addRange(presentation, lastStart, length, lastAttribute);
    }
    
    @Nullable
    private TextAttribute getAttributeForElementAt(PsiFile psiFile, int offset, IColorManager colorManager) {
        offset = LineEndUtil.convertCrToOsOffset(fDocument.get(), fScanner.getTokenOffset());
        PsiElement psiElement = psiFile.findElementAt(offset);
        if (psiElement != null) {
            return getTextAttributeFor(psiElement, colorManager);
        }
        
        return null;
    }
    
    @Nullable
    private TextAttribute getTextAttributeFor(PsiElement element, IColorManager colorManager) {
        RGB tokenColor = null;
        int style = SWT.NORMAL;
        if (element instanceof LeafPsiElement) {
            IElementType elementType = ((LeafPsiElement)element).getElementType();
            if (JetTokens.SOFT_KEYWORDS.contains(elementType) || JetTokens.KEYWORDS.contains(elementType) || JetTokens.MODIFIER_KEYWORDS.contains(elementType)) {
                tokenColor = IColorConstants.KEYWORD;
                style = SWT.BOLD;
            } else if (JetTokens.STRINGS.contains(elementType)) {
                tokenColor = IColorConstants.STRING;
            } else if (JetTokens.COMMENTS.contains(elementType)) {
                tokenColor = IColorConstants.COMMENT;
            } else {
                tokenColor = IColorConstants.DEFAULT;
            }
        }
        
        if (tokenColor == null) {
            return null;
        }
        
        return new TextAttribute(colorManager.getColor(tokenColor), null, style);
    }
}


