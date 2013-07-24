package org.jetbrains.kotlin.ui.editors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Point;
import org.jetbrains.kotlin.core.log.KotlinLogger;

public class BracketInserter implements VerifyKeyListener {
    
    private final List<PairConfiguration> bracketConfigurations = new ArrayList<PairConfiguration>();
    private ISourceViewer viewer;
    
    private enum Type {
        OPEN,
        CLOSE
    }
    
    private static class PairConfiguration {
        private final char open;
        private final char close;

        public PairConfiguration(char open, char close) {
            this.open = open;
            this.close = close;
        }
        
        public char getOpen() {
            return open;
        }
        
        public char getClose() {
            return close;
        }
        
    }

    public void addBrackets(char open, char close) {
        bracketConfigurations.add(new PairConfiguration(open, close));
    }
    
    public void setSourceViewer(ISourceViewer sourceViewer) {
        viewer = sourceViewer;
    }

    @Override
    public void verifyKey(VerifyEvent event) {
        if (!event.doit) {
            return;
        }
        
        if (!processChar(event.character, Type.OPEN) ||
                !processChar(event.character, Type.CLOSE)) {
            event.doit = false;
            return;
        }
    }
    
    public boolean processChar(char ch, Type type) {
        PairConfiguration pairConfiguration = getConfiguration(ch, type);
        
        return pairConfiguration == null || process(viewer.getDocument(), viewer.getSelectedRange(), type, pairConfiguration);
    }
    
    private PairConfiguration getConfiguration(char ch, Type type) {
        for (PairConfiguration conf : bracketConfigurations) {
            if (type == Type.OPEN) {
                if (conf.getOpen() == ch)
                    return conf;
            } else if (type == Type.CLOSE) {
                if (conf.getClose() == ch)
                    return conf;
            }            
        }
        
        return null;
    }
    
    private boolean process(IDocument document, Point point, Type type, PairConfiguration configuration) {
        int offset = point.x;
        int length = point.y;
        
        try {
            if (type == Type.OPEN) {
                if (offset+length >= document.getLength() || document.getChar(offset + length) != configuration.getOpen()) {
                    char[] pair = new char[] { configuration.getOpen(), configuration.getClose() };
                    document.replace(offset, 0, String.valueOf(pair));
                }
                skip();
                return false;
            } else if (type == Type.CLOSE) {
                if (offset + length < document.getLength() && document.getChar(offset + length) == configuration.getClose()) {
                    skip();
                    return false;
                }
            }
        } catch (BadLocationException exc) {
            KotlinLogger.logError(exc);
        }
        
        return true;
    }
    
    private void skip() {
        StyledText textWidget = viewer.getTextWidget();
        textWidget.setCaretOffset(textWidget.getCaretOffset() + 1);
    }
    
}
