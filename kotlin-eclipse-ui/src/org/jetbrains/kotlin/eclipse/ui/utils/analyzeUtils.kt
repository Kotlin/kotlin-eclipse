package org.jetbrains.kotlin.eclipse.ui.utils

import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.ui.editors.KotlinEditor
import org.jetbrains.kotlin.core.utils.getBindingContext

fun KotlinEditor.getBindingContext(): BindingContext? =
    parsedFile?.getBindingContext()