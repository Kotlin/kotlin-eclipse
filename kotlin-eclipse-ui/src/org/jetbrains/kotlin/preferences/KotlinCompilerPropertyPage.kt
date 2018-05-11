package org.jetbrains.kotlin.preferences

import org.eclipse.core.runtime.CoreException
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Status
import org.eclipse.core.runtime.jobs.Job
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Composite
import org.eclipse.ui.dialogs.PropertyPage
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.core.Activator
import org.jetbrains.kotlin.core.preferences.CompilerPlugin
import org.jetbrains.kotlin.core.preferences.KotlinProperties
import org.jetbrains.kotlin.swt.builders.*
import org.jetbrains.kotlin.utils.LazyObservable

abstract class KotlinCompilerPropertyPage : PropertyPage() {
    protected abstract val kotlinProperties: KotlinProperties

    private val pluginEntries: Iterable<CompilerPlugin>
        get() = kotlinProperties.compilerPlugins.entries.filterNot(CompilerPlugin::removed)
    
    private var selectedPlugin by LazyObservable<CompilerPlugin?>({ null }) { _, _, value ->
        val editable = value != null
        editButton.enabled = editable
        removeButton.enabled = editable
    }
    
    private lateinit var editButton: View<Button>

    private lateinit var removeButton: View<Button>

    protected abstract fun rebuildTask(monitor: IProgressMonitor?)

    protected fun View<Composite>.createOptionsControls(operations: View<Composite>.() -> Unit = {}) =
            gridContainer(cols = 2) {
                label("JVM target version: ")
                enumPreference(kotlinProperties::jvmTarget, nameProvider = JvmTarget::description) {
                    layout(horizontalGrab = true)
                }
                group("Compiler plugins:", cols = 2) {
                    layout(horizontalSpan = 2, verticalGrab = true)
                    val list = checkList(::pluginEntries, selectionDelegate = ::selectedPlugin, style = SWT.BORDER) {
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
                    editButton = button("Edit") {
                        enabled = false
                        onClick {
                            selectedPlugin?.also { CompilerPluginDialog(shell, kotlinProperties.compilerPlugins, it).open() }
                            list.refresh()
                        }
                    }
                    removeButton = button("Remove") {
                        enabled = false
                        onClick {
                            selectedPlugin?.apply {
                                removed = true
                            }
                            list.refresh()
                        }
                    }
                }
                group("Additional compiler flags") {
                    layout(horizontalSpan = 2, verticalGrab = true)
                    textField(kotlinProperties::compilerFlags, style = SWT.MULTI) { layout(horizontalGrab = true, verticalGrab = true) }
                }
            }.apply(operations)

    final override fun performOk(): Boolean {
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
            rebuildTask(monitor)
            Status.OK_STATUS
        } catch (e: CoreException) {
            Status(Status.ERROR, Activator.PLUGIN_ID, "Error during build of the project", e)
        }
    }

}

