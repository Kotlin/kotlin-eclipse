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
 */
package org.jetbrains.kotlin.ui.editors

import org.eclipse.jface.text.IRegion
import org.eclipse.jface.text.hyperlink.IHyperlink
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.ui.editors.navigation.KotlinOpenDeclarationAction

class KotlinElementHyperlink(
    private val openAction: KotlinOpenDeclarationAction,
    private val region: IRegion,
    private val refExpression: KtReferenceExpression? = null
) : IHyperlink {
    override fun getHyperlinkRegion(): IRegion = region

    override fun getTypeLabel(): String? = null

    override fun getHyperlinkText(): String = HYPERLINK_TEXT

    override fun open() {
        refExpression?.let { openAction.run(it) } ?: openAction.run()
    }

    companion object {
        private const val HYPERLINK_TEXT = "Open Kotlin Declaration"
    }
}