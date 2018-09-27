package org.jetbrains.kotlin.preferences.style

import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.ProjectScope
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.ui.IWorkbenchPropertyPage
import org.jetbrains.kotlin.core.preferences.KotlinCodeStyleProperties
import org.jetbrains.kotlin.preferences.BasePropertyPage
import org.jetbrains.kotlin.preferences.views.codeStylePropertiesView
import org.jetbrains.kotlin.swt.builders.*
import org.jetbrains.kotlin.utils.LazyObservable
import kotlin.properties.Delegates

class ProjectCodeStylePropertyPage : BasePropertyPage(), IWorkbenchPropertyPage {
    // project must be lazy initialized, because getElement() called during construction of page object returns null
    val project: IProject by lazy { element.getAdapter(IProject::class.java) }

    private var overrideFlag by LazyObservable({ properties.globalsOverridden }) { _, _, v ->
        properties.globalsOverridden = v
        settingsView.enabled = v
    }

    private lateinit var settingsView: View<Composite>

    override val properties by lazy { KotlinCodeStyleProperties(ProjectScope(project)) }

    override fun createUI(parent: Composite): Control =
            parent.asView.apply {
                checkbox(::overrideFlag, "Enable project speciffic settings")
                separator { layout(horizontalGrab = true) }
                settingsView = codeStylePropertiesView(properties) {
                    enabled = properties.globalsOverridden
                }
            }.control
}