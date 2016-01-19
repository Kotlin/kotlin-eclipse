/*******************************************************************************
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *******************************************************************************/
package org.jetbrains.kotlin.ui.editors;

import org.eclipse.jdt.internal.ui.text.JavaWordFinder;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.texteditor.ITextEditor;
import org.jetbrains.kotlin.ui.editors.navigation.KotlinOpenDeclarationAction;
import org.jetbrains.kotlin.ui.editors.navigation.NavigationUtilsKt;

public class KotlinElementHyperlinkDetector extends AbstractHyperlinkDetector {
    
    @Override
    public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
        ITextEditor textEditor = (ITextEditor) getAdapter(ITextEditor.class);
        if (region == null || !(textEditor instanceof KotlinEditor)) {
            return null;
        }
        KotlinEditor editor = (KotlinEditor) textEditor;
        
        IAction openAction = textEditor.getAction(KotlinOpenDeclarationAction.OPEN_EDITOR_TEXT);
        if (!(openAction instanceof KotlinOpenDeclarationAction)) {
            return null;
        }
        
        int offset = region.getOffset();
        IRegion wordRegion = JavaWordFinder.findWord(textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput()), offset);
        if (wordRegion == null || wordRegion.getLength() == 0) {
            return null;
        }
        
        if (NavigationUtilsKt.getSelectedExpressionWithParsedFile(editor, offset) == null) {
            return null;
        }
        
        return new IHyperlink[] {
                new KotlinElementHyperlink((KotlinOpenDeclarationAction) openAction, wordRegion)
        };
    }
}
