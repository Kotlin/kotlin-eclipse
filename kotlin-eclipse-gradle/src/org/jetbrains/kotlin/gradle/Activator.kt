package org.jetbrains.kotlin.gradle

import org.osgi.framework.BundleActivator
import org.osgi.framework.BundleContext

class Activator: BundleActivator {
    override fun start(context: BundleContext?) {
        Activator.context = context
    }

    override fun stop(context: BundleContext?) {
        Activator.context = null
    }
    
    companion object {
        const val PLUGIN_ID = "org.jetbrains.kotlin.gradle"
        
        var context: BundleContext? = null
        		get
            private set
    }
}