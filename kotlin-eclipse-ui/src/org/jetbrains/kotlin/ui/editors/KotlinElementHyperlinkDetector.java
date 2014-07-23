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

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.text.JavaWordFinder;
import org.eclipse.jdt.ui.actions.SelectionDispatchAction;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.utils.EditorUtil;

public class KotlinElementHyperlinkDetector extends AbstractHyperlinkDetector {
    
    @Override
    public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
        ITextEditor textEditor = (ITextEditor) getAdapter(ITextEditor.class);
        if (region == null || !(textEditor instanceof JavaEditor)) {
            return null;
        }
        
        IAction openAction = textEditor.getAction("OpenEditor");
        if (!(openAction instanceof SelectionDispatchAction)) {
            return null;
        }
        
        int offset = region.getOffset();
        JavaEditor javaEditor = (JavaEditor) textEditor;
        
        JetReferenceExpression expression = OpenDeclarationAction.getSelectedExpression(
                javaEditor,
                (JetFile) KotlinPsiManager.INSTANCE.getParsedFile(EditorUtil.getFile(javaEditor)),
                offset);
        if (expression == null) {
            return null;
        }
        
        IDocumentProvider documentProvider = textEditor.getDocumentProvider();
        IEditorInput editorInput = textEditor.getEditorInput();
        IDocument document = documentProvider.getDocument(editorInput);
        
        IRegion wordRegion = JavaWordFinder.findWord(document, offset);
        if (wordRegion == null || wordRegion.getLength() == 0) {
            return null;
        }
        
        return new IHyperlink[] {
                new KotlinElementHyperlink(
                        expression,
                        (SelectionDispatchAction) openAction,
                        wordRegion)
        };
    }
}
