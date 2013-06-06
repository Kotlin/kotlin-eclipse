package org.jetbrains.kotlin.ui;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.jetbrains.kotlin.core.utils.KotlinFilesCollector;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {
    // The plug-in ID
    public static final String PLUGIN_ID = "org.jetbrains.kotlin.ui"; //$NON-NLS-1$

    // The shared instance
    private static Activator plugin;

    public Activator() {
        KotlinFilesCollector.collectForParsing();
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     */
    public static Activator getDefault() {
        return plugin;
    }
}
