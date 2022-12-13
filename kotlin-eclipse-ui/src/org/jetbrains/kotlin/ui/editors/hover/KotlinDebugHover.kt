package org.jetbrains.kotlin.ui.editors.hover

import org.eclipse.jdt.internal.debug.core.model.JDILocalVariable
import org.eclipse.jdt.internal.debug.ui.ExpressionInformationControlCreator
import org.eclipse.jdt.internal.debug.ui.JavaDebugHover
import org.eclipse.jface.text.IInformationControlCreator
import org.jetbrains.kotlin.ui.editors.KotlinEditor

class KotlinDebugHover(): JavaDebugHover(), KotlinEditorTextHover<JDILocalVariable?> {

    override val hoverPriority: Int
        get() = 1

    private val fHoverControlCreator: IInformationControlCreator by lazy {
        ExpressionInformationControlCreator()
    }

    override fun isAvailable(hoverData: HoverData): Boolean = true

    override fun getHoverControlCreator(editor: KotlinEditor): IInformationControlCreator? = fHoverControlCreator

    override fun getHoverControlCreator(): IInformationControlCreator = fHoverControlCreator

    override fun getHoverInfo(hoverData: HoverData): JDILocalVariable? =
            getHoverInfo2(hoverData.editor.javaEditor.viewer, hoverData.getRegion()) as? JDILocalVariable

}