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

import java.util.concurrent.TimeUnit
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.equinox.p2.metadata.IInstallableUnit
import org.eclipse.equinox.p2.operations.OperationFactory
import org.eclipse.core.runtime.NullProgressMonitor
import java.net.URI
import org.eclipse.jface.dialogs.MessageDialog
import org.eclipse.swt.widgets.Display
import org.eclipse.equinox.p2.operations.ProfileModificationJob
import org.eclipse.equinox.p2.operations.ProvisioningJob
import org.eclipse.mylyn.commons.ui.dialogs.AbstractNotificationPopup
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.widgets.Link
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.equinox.p2.operations.UpdateOperation
import java.net.URL
import java.net.URLConnection
import java.net.HttpURLConnection
import org.jetbrains.kotlin.core.log.KotlinLogger
import com.intellij.openapi.util.SystemInfo
import java.net.URLEncoder
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.equinox.p2.metadata.Version
import java.util.Random
import org.jetbrains.kotlin.eclipse.ui.utils.runJob
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Status
import java.io.IOException
import org.eclipse.core.runtime.jobs.JobChangeAdapter
import org.eclipse.core.runtime.jobs.IJobChangeEvent
import org.eclipse.ui.PlatformUI

private sealed class PluginUpdateStatus() {
    object Update : PluginUpdateStatus()
    
    object CheckUpdateSiteFailed : PluginUpdateStatus()
    
    class UpdateFailed(val message: String) : PluginUpdateStatus()
}

public object KotlinPluginUpdater {
    val LAST_UPDATE_CHECK = "kotlin.lastUpdateCheck"
    val USER_ID = "kotlin.userId"
    
    private val KOTLIN_GROUP_ID = "org.jetbrains.kotlin.feature.feature.group"
    private val ECLIPSE_PLATFORM_ID = "org.eclipse.platform"
    
    private @Volatile var retry = false
    private @Volatile var LAST_RETRY_TIME = 0L
//    private val DELAY: Long = 3 * 1000 * 60 * 60 // 3 hours
    private val RETRY_DELAY: Long = TimeUnit.SECONDS.toMillis(5)
    private val MAIN_DELAY: Long = TimeUnit.SECONDS.toMillis(5)
    
    fun kotlinFileEdited() {
        val kotlinStore = Activator.getDefault().preferenceStore
        
        if (retry) {
            if (checkTimeIsUp(LAST_RETRY_TIME, RETRY_DELAY)) {
                val updateStatus = tryToUpdate(kotlinStore)
                processResults(updateStatus, kotlinStore)
            }
        } else {
            val lastUpdateTime = kotlinStore.getLong(LAST_UPDATE_CHECK)
            if (lastUpdateTime == 0L || checkTimeIsUp(lastUpdateTime, MAIN_DELAY)) {
                val updateStatus = tryToUpdate(kotlinStore)
                processResults(updateStatus, kotlinStore)
            }
        }
    }
    
    fun getKotlinInstallationUnit(monitor: IProgressMonitor): IInstallableUnit? {
        return OperationFactory().listInstalledElements(true, monitor).find { it.id == KOTLIN_GROUP_ID }
    }
    
    private fun checkTimeIsUp(lastTry: Long, delay: Long): Boolean = System.currentTimeMillis() - lastTry > delay
    
    private fun processResults(updateStatus: PluginUpdateStatus, kotlinStore: IPreferenceStore) {
        when (updateStatus) {
            is PluginUpdateStatus.Update -> {
                kotlinStore.setValue(LAST_UPDATE_CHECK, System.currentTimeMillis())
                retry = false
            }
            
            is PluginUpdateStatus.CheckUpdateSiteFailed -> setForRetry()
            
            is PluginUpdateStatus.UpdateFailed -> {
                KotlinLogger.logWarning("Could not check for update (${updateStatus.message})")
                setForRetry()
            }
        }
    }
    
    private fun setForRetry() {
        retry = true
        LAST_RETRY_TIME = System.currentTimeMillis()
    }
    
    private fun tryToUpdate(store: IPreferenceStore): PluginUpdateStatus {
        val monitor = NullProgressMonitor()
        val kotlinUnit = getKotlinInstallationUnit(monitor)
        if (kotlinUnit == null) {
            return PluginUpdateStatus.UpdateFailed("There is no kotlin feature group with id: $KOTLIN_GROUP_ID")
        }
        
        val os = URLEncoder.encode(SystemInfo.OS_NAME + " " + SystemInfo.OS_VERSION, "UTF-8")
        val pluginVersion = kotlinUnit.version
        val uid = getOrSetUID(store)
        val eclipseVersion = getEclipsePlatformVersion(monitor)
        if (eclipseVersion == null) {
            KotlinLogger.logWarning("There is no eclipse platform group with id: $ECLIPSE_PLATFORM_ID")
        }

        val updateSite: String?
        try {
            val params = "build=$eclipseVersion&pluginVersion=$pluginVersion&os=$os&uuid=$uid"
            updateSite = obtainUpdateSite("https://plugins.jetbrains.com/eclipse-plugins/kotlin/update?$params")
        } catch (e: IOException) {
            return PluginUpdateStatus.CheckUpdateSiteFailed
        }
        
        if (updateSite == null) {
            return PluginUpdateStatus.UpdateFailed("Could not obtain update site")
        }
        
        val resolvedUpdateSite = updateSite.removeSuffix("compositeArtifacts.xml")
        return proposeToUpdate(kotlinUnit, resolvedUpdateSite, monitor)
    }
    
    private fun proposeToUpdate(kotlinUnit: IInstallableUnit, updateSite: String, monitor: IProgressMonitor): PluginUpdateStatus {
        val uri = URI(updateSite)
        val updateOperation = OperationFactory().createUpdateOperation(listOf(kotlinUnit), listOf(uri), monitor)
        val result = updateOperation.resolveModal(monitor)
        if (result.isOK) {
            Display.getDefault().asyncExec { 
                val updateNotification = UpdatePluginNotification(updateOperation, monitor, Display.getDefault())
                updateNotification.open()
            }
        }
        
        return PluginUpdateStatus.Update
    }
    
    private fun getOrSetUID(store: IPreferenceStore): Long {
        val userId = store.getLong(USER_ID)
        if (userId == 0L) {
            val generatedId = Random().nextLong()
            store.setValue(USER_ID, generatedId)
            return generatedId
        }
        
        return userId
    }
    
    private fun getEclipsePlatformVersion(monitor: IProgressMonitor): Version? {
        return OperationFactory().listInstalledElements(false, monitor)
                .find { it.id == ECLIPSE_PLATFORM_ID }
                ?.version
    }
    
    private fun obtainUpdateSite(urlString: String): String? {
        val url = URL(urlString)
        val connection = url.openConnection()
        if (connection is HttpURLConnection) {
            try {
                connection.setInstanceFollowRedirects(false)
                connection.connect()
                
                if (connection.getResponseCode() == 302) {
                    return connection.getHeaderField("Location")
                }
            } finally {
                connection.disconnect()
            }
        }
        
        return null
    }
}

private class UpdatePluginNotification(
        val updateOperation: UpdateOperation,
        val monitor: IProgressMonitor,
        val display: Display) : AbstractNotificationPopup(display) {
    init {
        setDelayClose(0) // Don't close popup automatically
    }
    
    override fun createContentArea(parent: Composite) {
        parent.setLayout(GridLayout(1, true))
        
        val textLabel = Label(parent, SWT.LEFT)
        val installbleUnit = getInstallableUnit()
        textLabel.setText("A new version ${installbleUnit.version} of the Kotlin plugin is available.")
        textLabel.setLayoutData(gridData())
        
        val updateLink = Link(parent, SWT.RIGHT)
        updateLink.setText("<a>Update Plugin</a>")
        updateLink.setLayoutData(gridData(verticalAlignment = SWT.BOTTOM, grabExcessVerticalSpace = true))
        
        updateLink.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                val job = updateOperation.getProvisioningJob(monitor)
                job.addJobChangeListener(RestartJobAdapter(display))
                job.schedule()
                
                this@UpdatePluginNotification.close()
            }
        })
    }

    override fun getPopupShellTitle(): String = "Kotlin Plugin"
    
    private fun getInstallableUnit(): IInstallableUnit {
        return updateOperation.getProfileChangeRequest().getAdditions().first()
    }
}

private fun gridData(
            horizontalAlignment: Int = SWT.BEGINNING,
            verticalAlignment: Int = SWT.CENTER,
            grabExcessHorizontalSpace: Boolean = false,
            grabExcessVerticalSpace: Boolean = false,
            horizontalSpan: Int = 1,
            verticalSpan: Int = 1): GridData {
    return GridData(horizontalAlignment, verticalAlignment, grabExcessHorizontalSpace, grabExcessVerticalSpace, horizontalSpan, verticalSpan)
}

private class RestartJobAdapter(val display: Display) : JobChangeAdapter() {
    override fun done(event: IJobChangeEvent) {
        if (event.result.isOK) {
            display.syncExec { 
                val restart = MessageDialog.openQuestion(
                        null,
                        "Updates installed, restart?",
                        "Updates have been installed successfully, do you want to restart?");
                if (restart) {
                    PlatformUI.getWorkbench().restart()
                }
            }
        } else {
            display.syncExec { 
                MessageDialog.openError(null, "Error", event.result.message)
            }
        }
    }
}