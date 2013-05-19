package org.jetbrains.kotlin.core;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.jetbrains.kotlin.core.builder.ResourceChangeListener;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
	private static BundleContext context;
	
	private final IResourceChangeListener resourceChangeListener = new ResourceChangeListener();

	public static final String PLUGIN_ID = "org.jetbrains.kotlin.core";
	
	static BundleContext getContext() {
		return context;
	}

	@Override
    public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;
		
		getWorkspace().addResourceChangeListener(resourceChangeListener, IResourceChangeEvent.POST_CHANGE);
	}

	@Override
    public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
		
		getWorkspace().removeResourceChangeListener(resourceChangeListener);
	}
}
