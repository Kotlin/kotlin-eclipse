/*******************************************************************************
 * Copyright 2000-2015 JetBrains s.r.o.
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
package org.jetbrains.kotlin.eclipse.ui.utils

import com.intellij.psi.PsiElement
import org.eclipse.jface.text.IDocument
import org.jetbrains.kotlin.psi.JetFile

fun PsiElement.getEndLfOffset(document: IDocument): Int {
	return LineEndUtil.convertLfToDocumentOffset(this.getContainingFile().getText(), this.getTextRange().getEndOffset(), document)
}

fun PsiElement.getTextDocumentOffset(document: IDocument): Int {
    return getOffsetByDocument(document, getTextOffset())
}

fun PsiElement.getOffsetByDocument(document: IDocument, psiOffset: Int): Int {
    return LineEndUtil.convertLfToDocumentOffset(this.getContainingFile().getText(), psiOffset, document)
}

fun JetFile.findElementByDocumentOffset(offset: Int, document: IDocument): PsiElement? {
    val offsetWithoutCr = LineEndUtil.convertCrToDocumentOffset(document, offset);
    return this.findElementAt(offsetWithoutCr)
}