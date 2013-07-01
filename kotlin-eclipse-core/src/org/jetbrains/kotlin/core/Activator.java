package org.jetbrains.kotlin.core;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.runtime.Plugin;
import org.jetbrains.kotlin.core.builder.ResourceChangeListener;
import org.osgi.framework.BundleContext;

public class Activator extends Plugin {
    
	private static Activator plugin;
	
	private final IResourceChangeListener resourceChangeListener = new ResourceChangeListener();

	public static final String PLUGIN_ID = "org.jetbrains.kotlin.core";

	public Activator() {
	    plugin = this;
	}
	
	public static Activator getDefault() {
		return plugin;
	}

	@Override
    public void start(BundleContext bundleContext) throws Exception {
		getWorkspace().addResourceChangeListener(resourceChangeListener, IResourceChangeEvent.POST_CHANGE);
		
		super.start(bundleContext);
	}

	@Override
    public void stop(BundleContext bundleContext) throws Exception {
		getWorkspace().removeResourceChangeListener(resourceChangeListener);
		
		plugin = null;
	}
}
