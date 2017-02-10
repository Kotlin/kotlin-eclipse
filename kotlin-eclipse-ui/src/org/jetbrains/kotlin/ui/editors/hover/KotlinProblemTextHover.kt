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
package org.jetbrains.kotlin.ui.editors.hover

import org.eclipse.jdt.internal.ui.text.java.hover.AbstractAnnotationHover
import org.eclipse.jface.text.IInformationControlCreator
import org.eclipse.jface.text.Region
import org.jetbrains.kotlin.eclipse.ui.utils.getOffsetByDocument
import org.jetbrains.kotlin.ui.editors.KotlinEditor

class KotlinAnnotationTextHover : KotlinEditorTextHover<Any> {
    private val problemHover = object : AbstractAnnotationHover(true) {}
    
    override fun getHoverInfo(hoverData: HoverData): Any? {
        val region = hoverData.getRegion() ?: return null
        return problemHover.getHoverInfo2(hoverData.editor.javaEditor.viewer, region)
    }

    override fun isAvailable(hoverData: HoverData): Boolean = true
    
    override fun getHoverControlCreator(editor: KotlinEditor): IInformationControlCreator? {
        return problemHover.getHoverControlCreator()
    }
}