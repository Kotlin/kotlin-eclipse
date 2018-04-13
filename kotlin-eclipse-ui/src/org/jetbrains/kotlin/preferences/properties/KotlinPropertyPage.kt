package org.jetbrains.kotlin.preferences.properties

import org.eclipse.ui.dialogs.PropertyPage
import org.eclipse.ui.IWorkbenchPropertyPage
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Label
import org.eclipse.core.resources.IResource
import org.jetbrains.kotlin.core.log.KotlinLogger
import kotlin.reflect.KProperty
import org.eclipse.core.runtime.QualifiedName
import org.eclipse.core.resources.ProjectScope
import org.eclipse.core.resources.IProject
import org.eclipse.core.runtime.preferences.IEclipsePreferences
import org.jetbrains.kotlin.core.Activator
import org.jetbrains.kotlin.core.preferences.Preferences
import java.awt.GridLayout
import org.jetbrains.kotlin.swt.builders.*
import org.jetbrains.kotlin.core.preferences.KotlinProperties
import org.jetbrains.kotlin.config.JvmTarget
import org.eclipse.swt.layout.GridData
import org.eclipse.core.resources.IncrementalProjectBuilder
import org.eclipse.core.runtime.jobs.Job
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Status
import org.eclipse.core.runtime.CoreException

class KotlinPropertyPage : PropertyPage(), IWorkbenchPropertyPage {
	private val project: IProject by lazy { element.getAdapter(IProject::class.java) }
	
	private val kotlinProperties: KotlinProperties by lazy { KotlinProperties(ProjectScope(project)) }

	override fun createContents(parent: Composite): Control? = parent.gridContainer(cols = 2) {
		label("JVM target version: ")
		enumPreference(kotlinProperties::jvmTarget, nameProvider = JvmTarget::description) {
			layoutData = GridData(SWT.FILL, SWT.FILL, true, false)
		}
	}
	
	override fun performOk(): Boolean {
		kotlinProperties.flush()
		RebuildJob().apply {
			priority = Job.BUILD
			schedule()
		}
		return super.performOk()
	}
	
	private inner class RebuildJob: Job("Rebuilding workspace") {
		override fun run(monitor: IProgressMonitor?): IStatus = try {
			project.build(IncrementalProjectBuilder.FULL_BUILD, monitor)
			Status.OK_STATUS
		} catch (e: CoreException) {
			Status(Status.ERROR, Activator.PLUGIN_ID, "Error during build of the project", e)
		}
	}
	
}

