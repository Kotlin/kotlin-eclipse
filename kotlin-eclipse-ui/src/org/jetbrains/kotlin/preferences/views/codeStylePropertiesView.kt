package org.jetbrains.kotlin.preferences.views

import org.eclipse.swt.widgets.Composite
import org.jetbrains.kotlin.core.formatting.KotlinCodeStyleManager
import org.jetbrains.kotlin.core.preferences.KotlinCodeStyleProperties
import org.jetbrains.kotlin.swt.builders.*

fun View<Composite>.codeStylePropertiesView(
        properties: KotlinCodeStyleProperties,
        operations: View<Composite>.() -> Unit = {}
) =
        gridContainer(cols = 2) {
            label("Code style:")
            singleOptionPreference(
                    properties::codeStyleId,
                    KotlinCodeStyleManager.styles,
                    KotlinCodeStyleManager::getStyleLabel
            ) { layout(horizontalGrab = true) }
            operations()
        }
