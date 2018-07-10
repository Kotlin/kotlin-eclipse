package org.jetbrains.kotlin.preferences

import org.eclipse.swt.SWT
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.ui.dialogs.PropertyPage
import org.jetbrains.kotlin.core.preferences.Preferences
import org.jetbrains.kotlin.swt.builders.asView
import org.jetbrains.kotlin.swt.builders.layout

abstract class BasePropertyPage : PropertyPage() {
    protected abstract val properties: Preferences

    private lateinit var rootView: Composite

    protected abstract fun createUI(parent: Composite): Control

    protected open fun afterOk() {}

    final override fun createContents(parent: Composite): Control {
        rootView = Composite(parent, SWT.NONE).apply {
            layout = GridLayout().apply {
                marginWidth = 0
                marginHeight = 0
            }
        }

        createUI(rootView).asView
                .layout(horizontalGrab = true, verticalGrab = true)

        return rootView
    }

    final override fun performOk(): Boolean {
        properties.saveChanges()
        afterOk()
        return super.performOk()
    }

    final override fun performCancel(): Boolean {
        properties.cancelChanges()
        return super.performCancel()
    }

    final override fun performDefaults() {
        properties.loadDefaults()

        // Recreate view
        rootView.children.forEach { it.dispose() }
        createUI(rootView).asView
                .layout(horizontalGrab = true, verticalGrab = true)
        rootView.layout()

        super.performDefaults()
    }

}

