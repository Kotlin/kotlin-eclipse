/*******************************************************************************
 * Copyright 2000-2015 JetBrains s.r.o.
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
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.openapi.util.SystemInfo
import java.net.URLEncoder
import org.eclipse.core.runtime.Platform
import org.eclipse.ui.PlatformUI
import java.net.URL
import java.net.HttpURLConnection
import org.jetbrains.kotlin.core.log.KotlinLogger
import java.net.URLConnection
import org.osgi.framework.Version
import org.osgi.framework.Bundle

public object KotlinUsageReporter {
    @JvmField val LAST_UPDATE_KEY = "kotlin.lastReportCheck"
    @JvmField val UPDATE_USAGE_AVAILABLE_KEY = "kotlin.updateUsageReporting"
    @JvmField val ASK_FOR_USAGE_REPORTING_KEY = "kotlin.askForUsageReporting"

    fun kotlinFileEdited() {
        val kotlinStore = Activator.getDefault().preferenceStore
        val isUsageReportingAvailable = kotlinStore.getBoolean(UPDATE_USAGE_AVAILABLE_KEY)
        if (!isUsageReportingAvailable) return
        
        val lastUpdateTime = kotlinStore.getLong(LAST_UPDATE_KEY)
        if (lastUpdateTime == 0L || System.currentTimeMillis() - lastUpdateTime > TimeUnit.DAYS.toMillis(1)) {
            val isSuccessful = sendData()
            if (isSuccessful) {
                kotlinStore.setValue(LAST_UPDATE_KEY, System.currentTimeMillis())
            } else {
                KotlinLogger.logWarning("Could not send usage statistics")
            }
        }
    }
    
    fun getKotlinPluginVersion(): Version = Activator.getDefault().bundle.version
    
    fun getPlatformSymbolicName(): String = getPlatformBundle().getSymbolicName()
    
    fun getPlatformVersion(): Version = getPlatformBundle().getVersion()
    
    fun getOperatingSystem(): String = SystemInfo.OS_NAME + " " + SystemInfo.OS_VERSION
    
    private fun getPlatformBundle(): Bundle {
        val product = Platform.getProduct();
        return product.getDefiningBundle();
    }
    
    private fun sendData(): Boolean {
        val platformVersion = Platform.getBundle(PlatformUI.PLUGIN_ID).getVersion()
        val pluginVersion = Activator.getDefault().bundle.version
        val os = URLEncoder.encode(SystemInfo.OS_NAME + " " + SystemInfo.OS_VERSION, "UTF-8")
        val uid = 10
        
        val url = "some url, $platformVersion, $pluginVersion, $os, $uid"
        println("Send data")
        
        return send(url)
    }
    
    private fun send(urlString: String): Boolean {
        val url = URL(urlString)
        var connection: URLConnection? = null
        try {
            connection = url.openConnection()
            if (connection is HttpURLConnection) {
                return connection.getResponseCode() == 200
            }
        } finally {
            connection?.getInputStream()?.close()
        }
        
        return false
    }
}