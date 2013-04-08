package org.jetbrains.kotlin.ui;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.jetbrains.kotlin.core.builder.ResourceChangeListener;
import org.osgi.framework.BundleContext;
/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {
    // The plug-in ID
    public static final String PLUGIN_ID = "org.jetbrains.kotlin.ui"; //$NON-NLS-1$

    // The shared instance
    private static Activator plugin;
    
    private final IResourceChangeListener resourceChangeListener = new ResourceChangeListener();

    public Activator() {
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        
        getWorkspace().addResourceChangeListener(resourceChangeListener, IResourceChangeEvent.POST_CHANGE);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
        
        getWorkspace().removeResourceChangeListener(resourceChangeListener);
    }

    /**
     * Returns the shared instance
     */
    public static Activator getDefault() {
        return plugin;
    }
}
