package org.jetbrains.kotlin.common

import org.eclipse.ui.plugin.AbstractUIPlugin
import org.osgi.framework.BundleContext

class FormatterPlugin : AbstractUIPlugin() {
    companion object {
        val PLUGIN_ID = "formatter-common" //$NON-NLS-1$
        
        private var myDefault: FormatterPlugin? = null
    }
    
    override fun start(context: BundleContext?) {
        super.start(context)
        myDefault = this
    }
    
    override fun stop(context:BundleContext?) {
        myDefault = null
        super.stop(context)
    }
    
    fun getDefault(): FormatterPlugin? = myDefault
}