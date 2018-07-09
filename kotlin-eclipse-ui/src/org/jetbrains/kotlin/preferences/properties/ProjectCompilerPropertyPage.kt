package org.jetbrains.kotlin.preferences.properties

import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IncrementalProjectBuilder
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.ui.IWorkbenchPropertyPage
import org.jetbrains.kotlin.core.model.KotlinEnvironment
import org.jetbrains.kotlin.preferences.KotlinCompilerPropertyPage
import org.jetbrains.kotlin.swt.builders.*
import org.jetbrains.kotlin.utils.LazyObservable

class ProjectCompilerPropertyPage : KotlinCompilerPropertyPage(), IWorkbenchPropertyPage {
    // project must be lazy initialized, because getElement() called during construction of page object returns null
    val project: IProject by lazy { element.getAdapter(IProject::class.java) }

    override val kotlinProperties by lazy { KotlinEnvironment.getEnvironment(project).projectCompilerProperties }

    private var overrideFlag by LazyObservable({ kotlinProperties.globalsOverridden }) { _, _, value ->
        kotlinProperties.globalsOverridden = value
        optionsControls.enabled = value
    }

    private lateinit var optionsControls: View<Composite>

    override fun createUI(parent: Composite): Control =
            parent.asView.gridContainer {
                checkbox(::overrideFlag,"Enable project specific settings")
                separator()
                optionsControls = createOptionsControls {
                    enabled = kotlinProperties.globalsOverridden
                    layout(horizontalGrab = true, verticalGrab = true)
                }
            }.control
        
    override fun rebuildTask(monitor: IProgressMonitor?) {
        KotlinEnvironment.removeEnvironment(project)
        project.build(IncrementalProjectBuilder.FULL_BUILD, monitor)
    }
}