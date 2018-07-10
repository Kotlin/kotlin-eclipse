package org.jetbrains.kotlin.preferences

import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IncrementalProjectBuilder
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.ui.IWorkbenchPropertyPage
import org.jetbrains.kotlin.core.model.KotlinEnvironment
import org.jetbrains.kotlin.preferences.views.projectCompilerPropertiesView
import org.jetbrains.kotlin.swt.builders.asView

class ProjectCompilerPropertyPage : BasePropertyPage(), IWorkbenchPropertyPage {
    // project must be lazy initialized, because getElement() called during construction of page object returns null
    val project: IProject by lazy { element.getAdapter(IProject::class.java) }

    override val properties by lazy { KotlinEnvironment.getEnvironment(project).projectCompilerProperties }

    override fun createUI(parent: Composite): Control =
            parent.asView
                    .projectCompilerPropertiesView(properties) {
                        onIsValidChanged = { setValid(it) }
                    }
                    .control

    override fun afterOk() {
        RebuildJob { monitor ->
            KotlinEnvironment.removeEnvironment(project)
            project.build(IncrementalProjectBuilder.FULL_BUILD, monitor)
        }.schedule()
    }
}