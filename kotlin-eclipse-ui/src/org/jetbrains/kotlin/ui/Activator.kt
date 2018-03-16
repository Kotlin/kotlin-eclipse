/*******************************************************************************
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *******************************************************************************/
package org.jetbrains.kotlin.ui

import org.eclipse.core.resources.IResourceChangeEvent
import org.eclipse.core.resources.IResourceChangeListener
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.jdt.core.IElementChangedListener
import org.eclipse.jdt.core.JavaCore
import org.eclipse.ui.plugin.AbstractUIPlugin
import org.jetbrains.kotlin.ui.builder.KotlinClassPathListener
import org.jetbrains.kotlin.ui.builder.KotlinJavaDeclarationsListener
import org.jetbrains.kotlin.ui.builder.ResourceChangeListener
import org.osgi.framework.BundleContext

/**
 * The activator class controls the plug-in life cycle
 */
class Activator : AbstractUIPlugin() {
	private val resourceChangeListener by lazy { ResourceChangeListener() }
	private val scriptClasspathUpdater by lazy { ScriptClasspathUpdater() }
	private val kotlinClassPathChangedListener by lazy { KotlinClassPathListener() }
	private val kotlinJavaDeclarationsListener by lazy { KotlinJavaDeclarationsListener() }

	@Override
	override fun start(context: BundleContext): Unit {
		super.start(context);
		plugin = this;

		ResourcesPlugin.getWorkspace().addResourceChangeListener(
				resourceChangeListener,
				IResourceChangeEvent.POST_CHANGE.or(IResourceChangeEvent.PRE_CLOSE).or(IResourceChangeEvent.PRE_DELETE))
		ResourcesPlugin.getWorkspace().addResourceChangeListener(scriptClasspathUpdater, IResourceChangeEvent.POST_CHANGE)
		JavaCore.addElementChangedListener(kotlinClassPathChangedListener)
		JavaCore.addElementChangedListener(kotlinJavaDeclarationsListener)
	}

	@Override
	override fun stop(context: BundleContext): Unit {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener)
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(scriptClasspathUpdater)
		JavaCore.removeElementChangedListener(kotlinClassPathChangedListener)
		JavaCore.removeElementChangedListener(kotlinJavaDeclarationsListener)

		plugin = null
		super.stop(context)
	}

	/**
	 * Returns the shared instance
	 */
	companion object {
		// The plug-in ID
		val PLUGIN_ID = "org.jetbrains.kotlin.ui" //$NON-NLS-1$
		// The shared instance
		private var plugin: Activator? = null

		fun getDefault(): Activator {
			return plugin as Activator
		}
	}
}
