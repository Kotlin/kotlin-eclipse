/*******************************************************************************
 * Copyright 2000-2016 JetBrains s.r.o.
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
package org.jetbrains.kotlin.ui.tests.editors.completion

import org.eclipse.jface.text.contentassist.ICompletionProposal
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.jetbrains.kotlin.ui.editors.codeassist.KotlinCompletionProcessor
import org.jetbrains.kotlin.ui.editors.codeassist.KotlinCompletionProposal

fun getCompletionProposals(editor: KotlinFileEditor): Array<ICompletionProposal> {
    val processor = KotlinCompletionProcessor(editor, null, needSorting = true)
    val proposals = processor.computeCompletionProposals(editor.getViewer(), KotlinTestUtils.getCaret(editor))
    
    return proposals
}

fun ICompletionProposal.stringToInsert(): String {
    return if (this is KotlinCompletionProposal) replacementString else additionalProposalInfo ?: displayString
}