package org.jetbrains.kotlin.preferences.properties

import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IncrementalProjectBuilder
import org.eclipse.core.resources.ProjectScope
import org.eclipse.core.runtime.CoreException
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Status
import org.eclipse.core.runtime.jobs.Job
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.ui.IWorkbenchPropertyPage
import org.eclipse.ui.dialogs.PropertyPage
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.core.Activator
import org.jetbrains.kotlin.core.preferences.CompilerPlugin
import org.jetbrains.kotlin.core.preferences.KotlinProperties
import org.jetbrains.kotlin.swt.builders.*

class KotlinPropertyPage : PropertyPage(), IWorkbenchPropertyPage {
    // project must be lazy initialized, because getElement() called during creation of page returns null
    private val project: IProject by lazy { element.getAdapter(IProject::class.java) }

    private val kotlinProperties: KotlinProperties by lazy { KotlinProperties(ProjectScope(project)) }
    
    private val pluginEntries = { kotlinProperties.compilerPlugins.entries.filterNot(CompilerPlugin::removed) }

    override fun createContents(parent: Composite): Control? = parent.asView.gridContainer(cols = 2) {
        label("JVM target version: ")
        enumPreference(kotlinProperties::jvmTarget, nameProvider = JvmTarget::description) {
            layout(horizontalGrab = true)
        }
        group("Compiler plugins:", cols = 2) {
            layout(horizontalSpan = 2, verticalGrab = true)
            val list = checkList(pluginEntries, style = SWT.BORDER) {
                layout(horizontalGrab = true, verticalGrab = true, verticalSpan = 4)
                nameProvider = { it.key }
                checkDelegate = CompilerPlugin::active
            }
            button("Add") {
                onClick {
                    CompilerPluginDialog(control.shell, kotlinProperties.compilerPlugins, null).open()
                    list.refresh()
                }
            }
            button("Edit") {
                onClick {
                    list.selected?.also { CompilerPluginDialog(shell, kotlinProperties.compilerPlugins, it).open() }
                    list.refresh()
                }
            }
            button("Remove") {
                onClick {
                    list.selected?.apply {
                        removed = true
                    }
                    list.refresh()
                }
            }
        }
    }.control

    override fun performOk(): Boolean {
        kotlinProperties.compilerPlugins.entries
                .filter(CompilerPlugin::removed)
                .forEach(CompilerPlugin::remove)
        kotlinProperties.flush()
        RebuildJob().schedule()
        return super.performOk()
    }

    private inner class RebuildJob : Job("Rebuilding workspace") {

        init {
            priority = Job.BUILD
        }

        override fun run(monitor: IProgressMonitor?): IStatus = try {
            project.build(IncrementalProjectBuilder.FULL_BUILD, monitor)
            Status.OK_STATUS
        } catch (e: CoreException) {
            Status(Status.ERROR, Activator.PLUGIN_ID, "Error during build of the project", e)
        }
    }

}

